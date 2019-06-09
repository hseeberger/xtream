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

import akka.actor.{ ActorSystem => UntypedSystem }
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.{ TypedActorSystemOps, _ }
import akka.cluster.typed.{ Cluster, SelfUp, Subscribe, Unsubscribe }
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector
import org.apache.logging.log4j.scala.Logging
import pureconfig.generic.auto.exportReader
import pureconfig.loadConfigOrThrow

object Main extends Logging {

  private final case class Config(api: Api.Config)

  def main(args: Array[String]): Unit = {
    sys.props += "log4j2.contextSelector" -> classOf[AsyncLoggerContextSelector].getName // Always use async logging!

    val config = loadConfigOrThrow[Config]("xtream") // Must be first to aviod creating the actor system on failure!
    val system = UntypedSystem("xtream")
    system.spawn(Main(config), "main")
  }

  def apply(config: Config): Behavior[SelfUp] =
    Behaviors.setup { context =>
      logger.info(s"${context.system.name} started and ready to join cluster")

      val cluster = Cluster(context.system)
      cluster.subscriptions ! Subscribe(context.self, classOf[SelfUp])

      Behaviors.receive { (context, _) =>
        logger.info(s"${context.system.name} joined cluster and is up")

        cluster.subscriptions ! Unsubscribe(context.self)

        implicit val untypedSystem: UntypedSystem = context.system.toUntyped
        implicit val mat: Materializer            = ActorMaterializer()(context.system)

        Api(config.api)

        Behaviors.empty
      }
    }
}
