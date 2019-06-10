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

import akka.stream.{ Attributes, DelayOverflowStrategy, Materializer }
import akka.stream.scaladsl.{ FlowWithContext, Sink, Source }
import rocks.heikoseeberger.xtream.WordShuffler.ShuffleWord
import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

object TextShuffler {

  type Process =
    FlowWithContext[ShuffleText, Promise[TextShuffled], TextShuffled, Promise[TextShuffled], Any]

  final case class ShuffleText(text: String)
  final case class TextShuffled(text: String)

  final case class Config(delay: FiniteDuration)

  def apply(config: Config)(implicit mat: Materializer): Process = {
    import config._
    FlowWithContext[ShuffleText, Promise[TextShuffled]]
      .delay(delay, DelayOverflowStrategy.backpressure)
      .withAttributes(Attributes.inputBuffer(1, 1))
      .mapAsync(42) {
        case ShuffleText(text) =>
          Source
            .fromIterator(() => text.split(" ").iterator)
            .map(ShuffleWord)
            .via(WordShuffler())
            .map(_.text)
            .runWith(Sink.seq)
      }
      .map(words => TextShuffled(words.mkString(" ")))
  }
}
