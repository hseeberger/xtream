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

import akka.actor.Scheduler
import akka.pattern.after
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.concurrent.duration.FiniteDuration

object ExpiringPromise {

  final case class PromiseExpired(timeout: FiniteDuration)
      extends Exception(s"Promise not completed within $timeout!")

  def apply[A](timeout: FiniteDuration)(implicit ec: ExecutionContext,
                                        scheduler: Scheduler): Promise[A] = {
    val promise = Promise[A]()
    val expired = after(timeout, scheduler)(Future.failed(PromiseExpired(timeout)))
    promise.tryCompleteWith(expired)
    promise
  }
}
