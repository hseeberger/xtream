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

import akka.stream.scaladsl.Flow
import scala.annotation.tailrec
import scala.util.Random

object WordShuffler {

  type Process = Flow[ShuffleWord, WordShuffled, Any]

  final case class ShuffleWord(text: String)
  final case class WordShuffled(text: String)

  def apply(): Process =
    Flow[ShuffleWord]
      .map { case ShuffleWord(word) => WordShuffled(shuffleWord(word)) }

  def shuffleWord(word: String): String = {
    @tailrec def loop(word: String, acc: String = ""): String =
      if (word.isEmpty)
        acc
      else {
        val (left, right) = word.splitAt(Random.nextInt(word.length))
        val c             = right.head
        val nextWord      = left + right.tail
        loop(nextWord, c + acc)
      }
    if (word.length <= 3)
      word
    else {
      word.head + loop(word.tail.init) + word.last
    }
  }
}
