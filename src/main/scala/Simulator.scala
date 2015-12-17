import akka.actor.{ActorRef, Props, ActorSystem, Actor}
import akka.util.Timeout

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.{ ask, pipe };
/**
 * Created by gokul on 11/30/15.
 */
object Simulator extends App{
  override def main (args: Array[String]) {
    if (args.length != 3){
      println("Usage : \n\t Simulator <Number of Clients> <address of the REST server> <number of port to connect to>")
      System.exit(1)
    }
    var noOfUsers = args(0).toInt
    var host = args(1).toString
    var port = args(2).toInt
    implicit val system = ActorSystem("simulator");
    implicit val executionContext = system.dispatcher
    implicit val timeout = Timeout(100 seconds)
    var clients = ArrayBuffer[ActorRef]()
    val admin = system.actorOf(Props(classOf[Admin], host, port, noOfUsers), name = "Admin")
//    admin ! InitializeServer
    admin ! StartSimulation
//    admin ! PrintStatistics

  }
}

