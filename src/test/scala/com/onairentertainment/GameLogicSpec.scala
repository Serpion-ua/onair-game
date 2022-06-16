package com.onairentertainment

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class GameLogicSpec extends AnyWordSpecLike with Matchers {
  val maxValue = 999999
  val gameLogic = new GameLogic(System.nanoTime(), maxValue)

  "GameLogic" must {
    "generate random number for correct diapason" in {
      (1 to 1000).forall{_ =>
        val number = gameLogic.generateRandomNumber()
        number > 0 && number <= maxValue
      }.mustBe(true)
    }

    //@TODO check "generate random number shall have uniform distribution"

    "calculate numbers for five players" in {
      val playersNumber = 5
      val (players, bot) = gameLogic.generateRandomNumbers(playersNumber)
      players.length mustBe 5
      bot mustBe >= (0)
      bot mustBe <=(maxValue)
    }

    "calculate no numbers for zero players" in {
      val (players, bot) = gameLogic.generateRandomNumbers(0)
      players.length mustBe 0
      bot mustBe >= (0)
      bot mustBe <=(maxValue)
    }

    "calculate no numbers for negative players" in {
      val (players, bot) = gameLogic.generateRandomNumbers(-5)
      players.length mustBe 0
      bot mustBe >= (0)
      bot mustBe <=(maxValue)
    }

    "calculate correct result" in {
      gameLogic.calculateResult(966337)  mustBe 106
      gameLogic.calculateResult(964373)  mustBe 56
      gameLogic.calculateResult(4283)    mustBe 17
      gameLogic.calculateResult(0)       mustBe 0
      gameLogic.calculateResult(999999)  mustBe 900000
      gameLogic.calculateResult(-999999) mustBe 899999
    }

    "calculate correct results" in {
      val (playerNumbers, botNumber) = (Seq(966337, 964373, 4283, 0, 999999, -999999), 234)
      val expectedResults = (Seq(106, 56, 17, 0, 900000, 899999), 9)
      gameLogic.calculateResults(playerNumbers, botNumber) mustBe expectedResults
    }

    "calculate no results if no numbers" in {
      val (playerNumbers, botNumber) = (Seq(), 0)
      val expectedResults = (Seq(), 0)
      gameLogic.calculateResults(playerNumbers, botNumber) mustBe expectedResults
    }

    "calculate winners for correct results" in {
      val playerResults =
        Seq(PlayerNumberResult(1, 966337, 106), PlayerNumberResult(2, 964373, 56),
          PlayerNumberResult(3, 4283, 17), PlayerNumberResult(4, 0, 0), PlayerNumberResult(5, 999999, 900000))

      val fullPlayerResultAndPosition =
        Seq(
          GameResultForPlayer(1, 5, 999999, 900000),
          GameResultForPlayer(2, 1, 966337, 106),
          GameResultForPlayer(3, 2, 964373, 56),
          GameResultForPlayer(4, 3, 4283, 17),
          GameResultForPlayer(5, 4, 0, 0),
        )

      gameLogic.calculateWinners(playerResults, 0) mustBe fullPlayerResultAndPosition
      gameLogic.calculateWinners(playerResults, 106) mustBe fullPlayerResultAndPosition.slice(0, 2)
    }

    "correct start a game if game config is correct" in {
      gameLogic.startGame(5) mustBe a [Right[_, _]]
    }

    "doesn't start a game if game config is not correct" in {
      gameLogic.startGame(0) mustBe a [Left[_, _]]
    }
  }
}
