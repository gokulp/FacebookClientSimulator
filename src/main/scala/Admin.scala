import akka.actor.{Props, ActorRef, ActorSystem, Actor}
import akka.actor.Actor.Receive
import akka.util.Timeout
import spray.client.pipelining._
import spray.http.{HttpResponse, HttpRequest}
import scala.concurrent.duration._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Await}
import akka.pattern.{ ask, pipe }

import scala.util.{Failure, Success}
;
/**
 * Created by gokul on 12/1/15.
 */
class Admin(host:String, port:Int, noOfUsers:Int) extends Actor {
  implicit val system = ActorSystem("simulator");
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(100 seconds)
  var clients = ArrayBuffer[ActorRef]()
  var startTime = System.currentTimeMillis()
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  override def receive: Receive = {
    case StartSimulation =>
      println("Initializing respective clients!")
      for (i <- 0 until noOfUsers){
        clients += system.actorOf(Props(classOf[NewClient], host, port, i, 1, true,false, noOfUsers,self), name = i.toString)
//        var future = ask(clients(i) , GetProfile)
//        var dummy = Await.result(future, timeout.duration)
//        var future1 = ask(clients(i) , GetFriends)
//        var dummy1 = Await.result(future1, timeout.duration)
        clients(i)! "CreateProfile" //StartSimulatingUserBehavior
        //println("Done User "+i)
      }
      context.system.scheduler.scheduleOnce(10000 milliseconds, self, "MakeFriends")

    case "MakeFriends" =>
/*      for (i <- 0 until noOfUsers; j <- 0 until noOfUsers) {
        if (i!=j){
          clients(i) ! SendFriendRequest(j)
        }
      }*/
      for (i <- 0 until noOfUsers -1 ) {
        clients(i) ! SendFriendRequest(i+1)
      }
      context.system.scheduler.scheduleOnce(5000 milliseconds, self, "GetToken")
      context.system.scheduler.scheduleOnce(10000 milliseconds, self, "SeeRequests")

    case "SeeRequests" =>
      for (i <- 0 until noOfUsers) {
        clients(i) ! "GetPendingRequests"
      }
      context.system.scheduler.scheduleOnce(5000 milliseconds, self, "GetToken")
      context.system.scheduler.scheduleOnce(10000 milliseconds, self, "ProcessRequests")

    case "GetToken" =>
      for (i <- 0 until noOfUsers) {
        clients(i) ! "GetToken"
      }

    case "ProcessRequests" =>
      for (i <- 0 until noOfUsers) {
        clients(i) ! "ProcessRequest"
      }

    case PrintStatistics =>
      var interval:Long = (System.currentTimeMillis() - startTime)
      var response: Future[HttpResponse] = pipeline(Get("http://localhost:5001/Statistics/" + interval))
      response.onComplete {
        case Success(numberOfRequests) =>
          println("Number of Request processed after "+interval+"milliseconds are "+numberOfRequests)
        case Failure(error) =>
          //println(error, "failed to get profile")
          println("Requesting Statistics failed" + error)
      }
      context.system.scheduler.scheduleOnce(10000 milliseconds, self, PrintStatistics)


  }
}
