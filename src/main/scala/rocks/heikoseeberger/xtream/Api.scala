/*
 * Copyright 2019 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rocks.heikoseeberger.xtream

import akka.Done
import akka.actor.{ CoordinatedShutdown, ActorSystem => UntypedSystem }
import akka.actor.CoordinatedShutdown.{ PhaseServiceRequestsDone, PhaseServiceUnbind, Reason }
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import org.apache.logging.log4j.scala.Logging
import rocks.heikoseeberger.xtream.TextShuffler.ShuffleText
import scala.concurrent.duration.FiniteDuration
import scala.util.{ Failure, Success }

object Api extends Logging {

  final case class Config(hostname: String, port: Int, terminationDeadline: FiniteDuration)

  final object BindFailure extends Reason

  def apply(config: Config)(implicit untypedSystem: UntypedSystem, mat: Materializer): Unit = {
    import config._
    import untypedSystem.dispatcher

    val shutdown = CoordinatedShutdown(untypedSystem)

    Http()
      .bindAndHandle(route, hostname, port)
      .onComplete {
        case Failure(cause) =>
          logger.error(s"Shutting down, because cannot bind to $hostname:$port!", cause)
          shutdown.run(BindFailure)

        case Success(binding) =>
          logger.info(s"Listening for HTTP connections on ${binding.localAddress}")
          shutdown.addTask(PhaseServiceUnbind, "api.unbind") { () =>
            binding.unbind()
          }
          shutdown.addTask(PhaseServiceRequestsDone, "api.terminate") { () =>
            binding.terminate(terminationDeadline).map(_ => Done)
          }
      }

    def route: Route = {
      import akka.http.scaladsl.server.Directives._

      path("shuffle-text") {
        get {
          parameter("text") { text =>
            val shuffledText = TextShuffler.singleRequest(ShuffleText(text)).map(_.text)
            complete(shuffledText)
          }
        }
      }
    }
  }
}
