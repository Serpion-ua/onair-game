package com.onairentertainment

import java.util.Random

final case class GameResultForPlayer(position: Int, player: Int, number: Int, result: Int)
final case class PlayerNumberResult(player: Int, number: Int, result: Int)

object GameLogic {
  val playersCountErrorMessage: String = "Number of players shall be more than 1"
}

class GameLogic(seed: Long, maxValue: Int) {
  private val rnd = new Random(seed)

  def startGame(players: Int): Either[String, Seq[GameResultForPlayer]] = {
    if (players > 0)
      Right(getGameResults(players))
    else
      Left(GameLogic.playersCountErrorMessage)
  }

  def getGameResults(players: Int): Seq[GameResultForPlayer] = {
    val (playerNumbers, botNumber) = generateRandomNumbers(players)
    val (playerResults, botResult) = calculateResults(playerNumbers, botNumber)
    val playerIds = LazyList.from(1)

    val playerNumberResults =
      (playerNumbers zip playerResults zip playerIds)
        .map{case ((number, result), player) => PlayerNumberResult(player, number, result)}

    calculateWinners(playerNumberResults, botResult)
  }

  /**
   * Accepts number of players, e.g. 5 and generates a random number for each player & a bot player
   * @param players number of players
   * @return random number for each player & a bot player
   */
  def generateRandomNumbers(players: Int): (Seq[Int], Int) = {
    val playerNumbers = (1 to players).map(_ => generateRandomNumber())
    val botNumber = generateRandomNumber()
    (playerNumbers, botNumber)
  }

  /**
   * Generates a random number from 0 to maxValue
   * @return random number
   */
  def generateRandomNumber(): Int = {
    rnd.nextInt(maxValue + 1) //+1 due nextInt take exclusive bound
  }

  /**
   * Calculate results for player numbers and bot number
   * @param playersNumbers player numbers
   * @param botNumber bot number
   * @return results for players and bot
   */
  def calculateResults(playersNumbers: Seq[Int], botNumber: Int): (Seq[Int], Int) =
    (playersNumbers.map(calculateResult), calculateResult(botNumber))

  /**
   * constructs a game result with the following rules:
   * - counts occurrences of each digit in given number,
   * e.g. for number 447974 there are 4 - 3 times, 7 - 2 times, 9 - one time
   * - calculates result for each digit by formula 10 pow (times-1) * digit,
   * e.g. in number 447974 it will be 10 * 10 * 4 for 4, 10 * 7 for 7, 9 for 9
   * - summarizes all digit result, e.g. for number 447974 it will be 10 * 10 * 4 + 10 * 7 + 9 = 479
   * @param number source for result generation
   * @return result for number
   */
  def calculateResult(number: Int): Int = {
    val numberToOccurrence = number.toString.map(_.asDigit).groupMapReduce(identity)(_ => 1)(_ + _)

    //@TODO Calculations could be cached
    numberToOccurrence
      .map { case (number, occurrence) => (number * scala.math.pow(10, occurrence - 1)).toInt }
      .sum
  }

  /**
   * calculates winning list:
   * - all results that are below bot player aren't included into result list
   * - all winners should be sorted by position
   * @param playerResults PlayerNumberResult for players
   * @param botResult Bot result
   * @return winner list
   */
  def calculateWinners(playerResults: Seq[PlayerNumberResult], botResult: Int): Seq[GameResultForPlayer] = {
    playerResults
      .filter{_.result >= botResult }
      .sortBy(_.result)(Ordering.Int.reverse)
      .zip(LazyList.from(1))
      .map{case (PlayerNumberResult(player, number, result), position) =>
        GameResultForPlayer(position, player, number, result)
      }
  }

}
