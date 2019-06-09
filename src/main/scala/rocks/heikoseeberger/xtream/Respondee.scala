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

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

object Respondee {

  sealed trait Command
  final case class Response[A](a: A) extends Command
  private final case object Timeout  extends Command

  final case class ResponseTimeoutException(timeout: FiniteDuration)
      extends Exception(s"No response within $timeout!")

  def apply[A](response: Promise[A], responseTimeout: FiniteDuration): Behavior[Response[A]] =
    Behaviors
      .withTimers[Command] { timers =>
        timers.startSingleTimer("response-timeout", Timeout, responseTimeout)

        Behaviors.receiveMessage {
          case Timeout =>
            response.failure(ResponseTimeoutException(responseTimeout))
            Behaviors.stopped

          case Response(a: A @unchecked) =>
            response.success(a)
            Behaviors.stopped
        }
      }
      .narrow
}
