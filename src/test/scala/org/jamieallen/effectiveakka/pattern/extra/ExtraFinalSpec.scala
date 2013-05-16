package org.jamieallen.effectiveakka.pattern.extra

import akka.testkit.{ TestKit, TestProbe, ImplicitSender }
import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import org.scalatest.WordSpecLike
import org.scalatest.matchers.MustMatchers
import scala.concurrent.duration._
import org.jamieallen.effectiveakka.common._

class ExtraFinalSpec extends TestKit(ActorSystem("TestAS")) with ImplicitSender with WordSpecLike with MustMatchers {
  "An AccountBalanceRetriever" should {
    "return a list of account balances" in {
      val savingsAccountProxy = system.actorOf(Props[SavingsAccountProxy])
      val checkingAccountProxy = system.actorOf(Props[CheckingAccountProxy])
      val moneyMarketAccountProxy = system.actorOf(Props[MoneyMarketAccountsProxy])
      val probe = TestProbe()

      val accountBalanceRetriever = system.actorOf(Props(new AccountBalanceRetrieverFinal(savingsAccountProxy, checkingAccountProxy, moneyMarketAccountProxy)))
      accountBalanceRetriever.tell(GetCustomerAccountBalances(1L), probe.ref)
      val result = probe.expectMsgType[AccountBalances]
      result must equal(AccountBalances(Some(List((3, 15000))), Some(List((1, 150000), (2, 29000))), Some(List())))
    }

    "return a TimeoutException when timeout is exceeded" in {
      // Write a local stub to inject that will cause a timeout to occur
      val savingsAccountProxy = system.actorOf(Props(new Actor() with ActorLogging {
        def receive = {
          case GetCustomerAccountBalances(id: Long) =>
            log.debug(s"Received GetCustomerAccountBalances for ID: $id, going to force TIMEOUT!")
            Thread.sleep(1000)
        }
      }))
      val checkingAccountProxy = system.actorOf(Props[CheckingAccountProxy])
      val moneyMarketAccountProxy = system.actorOf(Props[MoneyMarketAccountsProxy])
      val probe = TestProbe()

      val accountBalanceRetriever = system.actorOf(Props(new AccountBalanceRetrieverFinal(savingsAccountProxy, checkingAccountProxy, moneyMarketAccountProxy)))
      accountBalanceRetriever.tell(GetCustomerAccountBalances(1L), probe.ref)
      probe.expectMsgType[String]

      //      probe.expectMsgType[AccountBalanceRetrieverFinal.AccountRetrievalTimeout]
    }
  }
}