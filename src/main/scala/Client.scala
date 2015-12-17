import akka.actor.{ActorRef, Actor}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import org.json4s.{DefaultFormats, Formats}
import spray.can.Http
import spray.client.pipelining
import spray.client.pipelining._
import spray.http._
import spray.httpx.marshalling.Marshaller

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.{Random, Success, Failure}
import entities._

import spray.httpx.Json4sSupport
import akka.pattern.{ ask, pipe };

/*
import spray.json.{JsonFormat, DefaultJsonProtocol}

object FacebookJsonProtocol extends DefaultJsonProtocol {
  implicit val profileFormat = jsonFormat2(Profile)
  implicit val postFormat = jsonFormat2(entities.Post)
  //implicit def googleApiResultFormat[T :JsonFormat] = jsonFormat2(GoogleApiResult.apply[T])
}
*/

/**
 * Created by gokul on 11/30/15.
 */

class Client (host: String , bindport: Int, id:Int, aggressionLevel: Int, isClient:Boolean, isPage:Boolean, numClients:Int,parent:ActorRef/*3: Reader 2: Normal User 1: Aggressive poster*/) extends Actor with Json4sSupport {
  implicit val system = context.system

  import system.dispatcher

  //var myProfile: Profile = new Profile(0, "1988-02-09", List("inClient@dummy.com"), "dummy", "Male", "dummy", "publick_key", List(), List())
  var myProfile: Profile = new Profile(0, "", List(""),
    "", "", "",
     List(), List(), List(), List(), "", "")
  var friendList: FriendList = new FriendList(0, List())
  var newsFeed = new ArrayBuffer[UserPost]()
  //var page: Page = new Page(0, "", false, false, List(), "", 0)

  var failedAttempts: Int = 0
  implicit val timeout = Timeout(100 seconds)
  var key = RSAEncryptor.generateKey();

  var myPhoto:Photo = new Photo(0,0,"","",-1,false)
  var myPost:UserPost = new UserPost(0,0,"","","")
  var myAlbum:Album = new Album(0,0,0,"",false,"",List())
  var myFriendList:FriendList = new FriendList(0, List())
  //var myPage:Page = new Page(0,"",false,false,List(),"",0)
  var myPage: Page = new  Page(0, "", 0, 0, List(),
    "",0,List(), List(), List(), "")


  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  val profilePipeline: HttpRequest => Future[Profile] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[Profile]
    )
  val friendPipeline: HttpRequest => Future[FriendList] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[FriendList]
    )
  val postPipeline: HttpRequest => Future[UserPost] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[UserPost]
    )
  val pagePipeline: HttpRequest => Future[Page] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[Page]
    )
  val photoPipeline: HttpRequest => Future[Photo] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[Photo]
    )
  val albumPipeline: HttpRequest => Future[Album] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[Album]
    )
  var name = self.actorRef.path.name
  var interval = Math.pow(10, aggressionLevel).toInt

  override def receive: Receive = {
    case StartSimulatingUserBehavior =>
      val noOfChoices = 6
      var choice = Random.nextInt(noOfChoices)
      choice match {
        case 0 =>
          self ! UpdateProfile(myProfile)
        case 1 =>
          self ! GetPost
        case 2 =>
          if (!myFriendList.list.isEmpty) {
            var pickFrnd = myFriendList.list(Random.nextInt(myFriendList.list.length))
            self ! GetFriendProfile(pickFrnd)
          }
        case 3 =>
          if (!myProfile.albums.isEmpty){
            var pickAlbum = myProfile.albums(Random.nextInt(myProfile.albums.length))
            self ! GetAlbum(pickAlbum)
          }
        case 4 =>
          if (!myProfile.photos.isEmpty){
            var pickPhoto = myProfile.photos(Random.nextInt(myProfile.photos.length))
            self ! GetPhoto(pickPhoto)
          }
        case 5 =>
          var dummyAlbumID = Random.nextInt(numClients)
          self ! GetAlbum(dummyAlbumID)
      }
      context.system.scheduler.scheduleOnce(interval milliseconds, self, StartSimulatingUserBehavior)
    case initiate(noOfUsers: Int) =>
      val obj = IntWrapper(noOfUsers)
      var response: Future[HttpResponse] = pipeline(Get("http://localhost:5001/initiateProfiles/" + noOfUsers))
      var dummy = Await.result(response, timeout.duration)
      response.onComplete {
        case Success(profile) =>
          println("Profile Initiation Successful Returned Success ")
        case Failure(error) =>
          ////println(error, "failed to get profile")
          //println("Profile Initialization failed -- shutting down the system " + error)
          context.stop(self)
          system.shutdown()
          System.exit(2);
      }
      response = pipeline(Get("http://localhost:5001/initiateFriends/" + noOfUsers))
      var dummy1 = Await.result(response, timeout.duration)
      response.onComplete {
        case Success(profile) =>
          println("Friend Initiation Successful Returned Success ")
        case Failure(error) =>
          ////println(error, "failed to get profile")
          //println("Friend Initialization failed -- shutting down the system " + error)
          context.stop(self)
          system.shutdown()
          System.exit(2);
      }

      response = pipeline(Get("http://localhost:5001/initiatePages/" + noOfUsers / 2))
      var dummy2 = Await.result(response, timeout.duration)
      response.onComplete {
        case Success(profile) =>
          println("Pages Initiation Successful Returned Success ")
        case Failure(error) =>
          ////println(error, "failed to get profile")
          //println("Pages Initialization failed -- shutting down the system " + error)
          context.stop(self)
          system.shutdown()
          System.exit(2);
      }

      response = pipeline(Get("http://localhost:5001/initiatePosts/" + noOfUsers))
      var dummy3 = Await.result(response, timeout.duration)
      response.onComplete {
        case Success(profile) =>
          println("UserPosts Initiation Successful Returned Success ")
        case Failure(error) =>
          ////println(error, "failed to get profile")
          //println("UserPost Initialization failed -- shutting down the system " + error)
          context.stop(self)
          system.shutdown()
          System.exit(2);
      }

      response = pipeline(Get("http://localhost:5001/initiateAlbums/" + noOfUsers))
      dummy3 = Await.result(response, timeout.duration)
      response.onComplete {
        case Success(profile) =>
          println("Album Initiation Successful Returned Success ")
        case Failure(error) =>
          ////println(error, "failed to get profile")
          //println("Album Initialization failed -- shutting down the system " + error)
          context.stop(self)
          system.shutdown()
          System.exit(2);
      }

      response = pipeline(Get("http://localhost:5001/initiatePhotos/" + noOfUsers))
      dummy3 = Await.result(response, timeout.duration)
      response.onComplete {
        case Success(profile) =>
          println("Photos Initiation Successful Returned Success ")
        case Failure(error) =>
          ////println(error, "failed to get profile")
          //println("Photos Initialization failed -- shutting down the system " + error)
          context.stop(self)
          system.shutdown()
          System.exit(2);
      }

      println("All Initialization successful on server!")
      sender ! 1; //Respond to parent after completion

    case AddFriend(userId: FriendListUpdate) =>
      if(name.toInt != userId) //cannot add self to friend list
      {
        val response: Future[FriendList] = friendPipeline(Put("http://localhost:5001/friendLists/" + name, userId))
        response.onComplete {
          case Success(response) =>
            //println("Returned Success " + response)
            myFriendList = response
            failedAttempts = 0
          case Failure(error) =>
            ////println(error, "failed to get profile")
            if (failedAttempts == 5)
              context.stop(self)
            else failedAttempts += 1
            context.system.scheduler.scheduleOnce(999 milliseconds, self, GetFriends)
        }
      }
    case UpdateProfile(profile:Profile) =>
      val response: Future[Profile] = profilePipeline(Put("http://localhost:5001/profiles/"+name, profile))
      response.onComplete{
        case Success(profile) =>
          ////println("Returned Success "+profile)
          myProfile = profile //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }
    case GetFriends =>
      val response: Future[FriendList] = friendPipeline(Get("http://localhost:5001/friendLists/"+name))
      response.onComplete{
        case Success(response) =>
          //println("Returned Success "+response)
          myFriendList = response
          failedAttempts = 0
        //self ! AddFriend(FriendListUpdate(list = 2))
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5) {
            failedAttempts += 1
            context.system.scheduler.scheduleOnce(999 milliseconds, self, GetFriends)
          }
      }
      sender ! parent
    case GetProfile =>
      ////println("Name of actor"+name)
      val response: Future[Profile] = profilePipeline(Get("http://localhost:5001/profiles/"+name))
      response.onComplete{
        case Success(profile) =>
          //println("Returned Success "+profile)
          myProfile = profile
          //self ! GetFriends
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5) {
            failedAttempts += 1
            context.system.scheduler.scheduleOnce(999 milliseconds, self, GetProfile)
          }
      }
      sender ! parent
    //case UpdateProfile =>
    case GetFriendProfile(userID:Int) =>
      ////println("Name of actor"+name)
      val response: Future[Profile] = profilePipeline(Get("http://localhost:5001/profiles/"+userID))
      response.onComplete{
        case Success(profile) =>
          //println("Returned Success "+profile)
          //myProfile = profile
          //self ! GetFriends
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
/*          if (failedAttempts == 5) {
            failedAttempts += 1
            context.system.scheduler.scheduleOnce(999 milliseconds, self, GetFriendProfile(userID))
          }*/
      }

    case GetPhoto(photoID) =>
      ////println("Name of actor"+name)
      val response: Future[Photo] = photoPipeline(Get("http://localhost:5001/photos/"+photoID))
      response.onComplete{
        case Success(photo) =>
          ////println("Returned Success "+photo)
          myPhoto = photo
          // self ! GetFriends                 : TO DO
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get photo")
/*
          if (failedAttempts == 5) {
            context.stop(self)
            //println("Shutting down actor "+name)
          }
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, GetPhoto(photoID))
*/
      }

    case GetAlbum(albumID) =>
      ////println("Name of actor"+name)
      val response: Future[Album] = albumPipeline(Get("http://localhost:5001/albums/"+albumID))
      response.onComplete{
        case Success(album) =>
          ////println("Returned Success "+album)
          myAlbum = album
          //self ! GetFriends
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get album")
/*
          if (failedAttempts == 5) {
            context.stop(self)
            //println("Shutting down actor "+name)
          }
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, GetAlbum(albumID))
*/
      }

    case GetPage =>
      ////println("Name of actor"+name)
      val response: Future[Page] = pagePipeline(Get("http://localhost:5001/pages/"+name))
      response.onComplete{
        case Success(page) =>
          //println("Returned Success "+page)
          myPage = page
          //self ! GetFriends
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get page")
          if (failedAttempts == 5) {
            context.stop(self)
            //println("Shutting down actor "+name)
          }
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, GetPage)
      }

    case GetPost =>
      ////println("Name of actor"+name)
      val response: Future[UserPost] = postPipeline(Get("http://localhost:5001/posts/"+name))
      response.onComplete{
        case Success(post) =>
          ////println("Returned Success "+post)
          myPost = post
          //self ! GetFriends
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get the post")
/*
          if (failedAttempts == 5) {
            context.stop(self)
            //println("Shutting down actor "+name)
          }
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, GetPost)
*/
      }

    case UpdateAlbum(album:AlbumUpdate) =>
      val response: Future[Album] = albumPipeline(Put("http://localhost:5001/albums/"+album.id, album))
      response.onComplete{
        case Success(album) =>
          //println("Returned Success "+album)
          myAlbum = album //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }

    case UpdateFriendList(friendList:FriendListUpdate) =>
      val response: Future[FriendList] = friendPipeline(Put("http://localhost:5001/frienlists/"+name, friendList))
      response.onComplete{
        case Success(friendList) =>
          //println("Returned Success "+friendList)
          myFriendList = friendList //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }

/*
    case UpdatePage(page:PageUpdate) =>
      val response: Future[Page] = pagePipeline(Put("http://localhost:5001/pages/"+page.id, page))
      response.onComplete{
        case Success(page) =>
          //println("Returned Success "+page)
          myPage = page //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }
*/


    case UpdatePhoto(photo:PhotoUpdate) =>
      val response: Future[Photo] = photoPipeline(Put("http://localhost:5001/photos/"+photo.id, photo))
      response.onComplete{
        case Success(photo) =>
          //println("Returned Success "+photo)
          myPhoto = photo //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }

    case UpdatePost(posts:UserPost) =>
      val response: Future[UserPost] = postPipeline(Put("http://localhost:5001/posts/"+posts.id, posts))
      response.onComplete{
        case Success(response) =>
          //println("Returned Success "+response)
          myPost = response //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }

    case DeleteProfile(profile:Profile) =>
      val response: Future[Profile] = profilePipeline(Delete("http://localhost:5001/profiles/"+profile.id))
      response.onComplete{
        case Success(response) =>
          //println("Profile Deleted "+response)
          myProfile = response //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }

    case DeletePhoto(photo:Photo) =>
      val response: Future[Photo] = photoPipeline(Delete("http://localhost:5001/photos/"+photo.id))
      response.onComplete{
        case Success(photo) =>
          //println("Photo Deleted "+photo)
          myPhoto = photo //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get profile")
      }


    case DeleteUserPost(post:UserPost) =>
      val response: Future[UserPost] = postPipeline(Delete("http://localhost:5001/posts/"+post.id))
      response.onComplete{
        case Success(post) =>
          //println("Post Deleted "+post)
          myPost = post //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get post")
      }

    case DeletePage(page:Page) =>
      val response: Future[Page] = pagePipeline(Delete("http://localhost:5001/pages/"+page.id))
      response.onComplete{
        case Success(page) =>
          //println("Page Deleted "+page)
          myPage = page //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get post")
      }

    case DeleteAlbum(album:Album) =>
      val response: Future[Album] = albumPipeline(Delete("http://localhost:5001/albums/"+album.id))
      response.onComplete{
        case Success(album) =>
          //println("Album Deleted "+ album)
          myAlbum = album //Update current profile with updated profile
          failedAttempts = 0
        case Failure(error) =>
        //Update failed
        ////println(error, "failed to get post")
      }

    case DeleteFriend(userId: FriendListUpdate) =>
      val response: Future[FriendList] = friendPipeline(Delete("http://localhost:5001/friendLists/" + name)) //TODO: Make changes to remove a friend from list
      response.onComplete {
        case Success(response) =>
          //println("Returned Success " + response)
          myFriendList = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, DeleteFriend)

      }

    case AddPost(post:UserPost) =>
      val response: Future[UserPost] = postPipeline(Post("http://localhost:5001/posts/" + name, post))
      response.onComplete {
        case Success(response) =>
          //println("Added Post " + response)
          myPost = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, AddPost(post:UserPost))
      }

    case AddPhoto(photo:Photo) =>
      val response: Future[UserPost] = postPipeline(Post("http://localhost:5001/photos/" + name, photo))
      response.onComplete {
        case Success(response) =>
          //println("Added Post " + response)
          myPost = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, AddPhoto(photo:Photo))
      }


    case AddAlbum(album:Album) =>
      val response: Future[Album] = albumPipeline(Post("http://localhost:5001/albums/" + name, album))
      response.onComplete {
        case Success(response) =>
          //println("Added Album " + response)
          myAlbum = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, AddAlbum(album:Album))
      }


    case AddPage(page:Page) =>
      val response: Future[Page] = pagePipeline(Post("http://localhost:5001/pages/" + name, page))
      response.onComplete {
        case Success(response) =>
          //println("Added Page " + response)
          myPage = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, AddPage(page:Page))
      }


    case AddProfile(profile:Profile) =>
      val response: Future[Profile] = profilePipeline(Post("http://localhost:5001/profiles/" + name, profile))
      response.onComplete {
        case Success(response) =>
          //println("Added Profile " + response)
          myProfile = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, AddProfile(profile:Profile))
      }
  }

  override implicit def json4sFormats: Formats = DefaultFormats
}