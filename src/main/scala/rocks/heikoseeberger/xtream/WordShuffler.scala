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

import akka.{ Done, NotUsed }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.{ KillSwitches, Materializer, SinkRef, UniqueKillSwitch }
import akka.stream.scaladsl.{ FlowWithContext, Keep, MergeHub, Sink, StreamRefs }
import akka.stream.typed.scaladsl.ActorMaterializer
import org.apache.logging.log4j.scala.Logging
import scala.annotation.tailrec
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Random, Success }

object WordShuffler extends Logging {

  type Process =
    FlowWithContext[ShuffleWord, Respondee[WordShuffled], WordShuffled, Respondee[WordShuffled], Any]

  sealed trait Command
  final case class GetSinkRef(replyTo: ActorRef[SinkRef[(ShuffleWord, Respondee[WordShuffled])]])
      extends Command
  final case object Shutdown     extends Command
  private final case object Stop extends Command

  final case class ShuffleWord(text: String)
  final case class WordShuffled(text: String)

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      implicit val mat: Materializer    = ActorMaterializer()(context.system)
      implicit val ec: ExecutionContext = context.executionContext
      val self                          = context.self
      val (sink, switch, done)          = runProcess()

      done.onComplete { reason =>
        logger.warn(s"Process completed: $reason")
        self ! Stop // Probably better to run the process again!
      }

      Behaviors.receiveMessagePartial {
        case GetSinkRef(replyTo) =>
          ???
          Behaviors.same

        case Shutdown =>
          switch.shutdown()
          Behaviors.receiveMessagePartial { case Stop => Behaviors.stopped }
      }
    }

  def process(): Process =
    FlowWithContext[ShuffleWord, Respondee[WordShuffled]]
      .map { case ShuffleWord(word) => WordShuffled(shuffleWord(word)) }

  def runProcess()(
      implicit mat: Materializer
  ): (Sink[(ShuffleWord, Respondee[WordShuffled]), NotUsed], UniqueKillSwitch, Future[Done]) =
    MergeHub
      .source[(ShuffleWord, Respondee[WordShuffled])](1)
      .viaMat(???)(Keep.both)
      .via(process())
      .toMat(Sink.foreach { case (wordShuffled, r) => r ! Respondee.Response(wordShuffled) }) {
        ???
      }
      .run()

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
