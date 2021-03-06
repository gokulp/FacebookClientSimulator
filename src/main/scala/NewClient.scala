import java.security.PublicKey

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import entities._
import org.apache.commons.codec.binary.Base64
import org.json4s.{DefaultFormats, Formats}
import spray.client.pipelining._
import spray.http._
import spray.httpx.Json4sSupport

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Promise, Await, Future, promise}
import scala.util.{Failure, Random, Success};

/**
 * Created by gokul on 11/30/15.
 */

class NewClient (host: String , bindport: Int, actorId:Int, aggressionLevel: Int, isClient:Boolean, isPage:Boolean,
                 numClients:Int,parent:ActorRef) extends Actor with Json4sSupport {
  implicit val system = context.system

  import system.dispatcher

  var userID = actorId

  /**
   * Security Related Information.
   */
  var symmetricKey = GenerateRandomStuff.getString(32)
  var initVector = "RandomInitVector"
  var key = RSAEncryptor.generateKey();

  var myProfile: Profile = new Profile(0, "", List(""),
    "", "", "",
    List(), List(), List(), List(), "")
  var friendList: FriendList = new FriendList(0, List(), List(), "")
  var pendingList:FriendList = friendList
  var newsFeed = new ArrayBuffer[UserPost]()
  //var myPage1: Page = new Page(0, "", false, false, List(), "", 0)
  var failedAttempts: Int = 0
  implicit val timeout = Timeout(100 seconds)


  var myPhoto:Photo = new Photo(0,0,0,"","",-1,false)
  var myPost:UserPost = new UserPost(0,0,0,"","","","",List())
  var myAlbum:Album = new Album(0,0,0,0,"",false,"",List())
  var myFriendList:FriendList = new FriendList(0, List(), List(), "")
  //var myPage:Page = new Page(0,"",false,false,List(),"",0)
  var myPage: Page = new  Page(0, "", 0, 0, List(),
    "",0,List(), List(), List())

  var myHiddenPhoto:PhotoWrapper = new PhotoWrapper(0,0,0,0,false,"","")
  var myHiddenPost:UserPostWrapper = new UserPostWrapper(0,0,0,"","","")
  var myHiddenProfile:ProfileWrapper = new ProfileWrapper(0,"","","",List(), List(), List(), List(),"","","")
  var myHiddenPage:PageWrapper = new PageWrapper(0,"",0,List(), List(), List(), "","")
  var tokenToSend:Authentication = new Authentication(userID, "")
  val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  val stringPipeline: HttpRequest => Future[Option[String]] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[Option[String]]
    )
  val tokenPipeline: HttpRequest => Future[Authentication] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[Authentication]
    )

  val profilePipeline: HttpRequest => Future[ProfileWrapper] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[ProfileWrapper]
    )
  val friendPipeline: HttpRequest => Future[FriendList] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[FriendList]
    )
  val postPipeline: HttpRequest => Future[UserPostWrapper] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[UserPostWrapper]
    )
  val pagePipeline: HttpRequest => Future[PageWrapper] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[PageWrapper]
    )
  val photoPipeline: HttpRequest => Future[PhotoWrapper] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[PhotoWrapper]
    )
  val albumPipeline: HttpRequest => Future[Album] = (
    addHeader("X-My-Special-Header", "fancy-value")
      ~> sendReceive
      ~> unmarshal[Album]
    )
  var name = self.actorRef.path.name
  var interval = Math.pow(10, aggressionLevel).toInt
  var serverPublicKey:PublicKey = key.getPublic
  var tokenNum:Long = 0

  def getToken():Unit = {
    val response:Future[Authentication] = tokenPipeline(Get("http://localhost:5001/getSecureToken/"+userID))
    response.onComplete{
      case Success(rsp)=>
        var token:String = rsp.token
        var num = RSAEncryptor.decrypt(Base64.decodeBase64(token), key.getPrivate)
        tokenNum = num.toString.toLong
        var tokenString = RSAEncryptor.signContent(tokenNum, key.getPrivate)
        tokenToSend = new Authentication(userID, tokenString)
      case Failure(error) =>
        println("Error while fetching secure token "+error)
        println("Trying again")
        self ! "GetToken"
    }
  }

  override def receive: Receive = {
    /**
     * To retrieve secure token from the server.
     */
    case "GetToken" =>
      getToken()

    /**
     * CreateProfile in order initialize actor as user actor we call create page from admin routine.
     */
    case "CreateProfile" =>
      var profile: Profile = new Profile(userID, GenerateRandomStuff.getDOB, List(GenerateRandomStuff.getEmail),
        GenerateRandomStuff.getName, GenerateRandomStuff.getGender(Random.nextInt(2)), GenerateRandomStuff.getName,
        List(), List(), List(), List(), key.getPublic.toString)
      self ! AddProfile(profile)
      myProfile = profile

    /**
     * CreatePage in order initialize actor as Page actor we call create page from admin routine.
     */
    case "CreatePage" =>
      var publickey = RSAEncryptor.getPublicKeyString(key.getPublic)
      var page: Page = new  Page(userID, GenerateRandomStuff.getName, 0, 0, List(GenerateRandomStuff.getEmail),
        GenerateRandomStuff.getName,GenerateRandomStuff.randBetween(100,10000),List(), List(), List())
      self ! AddPage(page)
      myPage = page

    /**
     * GetServerPublicKey: This is the very first step for any actor. Even before registering a profile.
     */
    case "GetServerPublicKey" =>
      val response:Future[Authentication] = tokenPipeline(Get("http://localhost:5001/getPublicKey"))
      response.onComplete{
        case Success(rsp)=>
          serverPublicKey = RSAEncryptor.getPublicKeyFromString(rsp.token)
          self ! "GetToken"
        case Failure(error) =>
          println("Error "+error)
          self ! "GetServerPublicKey"
      }

    /**
      * AddProfile is used to add new profile to the server. As every new profile can be created
     * fresh and there is no way to authenticate users in this case. We don't send any
     * authentication token. In real-world scenario this can be used terms and conditions signing part.
     */
    case AddProfile(profile:Profile) =>
      val hvalue:String = AESEncryptor.encrypt(symmetricKey,initVector,profile)
      val tempProfile: ProfileWrapper = new ProfileWrapper(profile.id, profile.first_name,profile.gender,profile.last_name,profile.albums, profile.photos, profile.likedpages, profile.userposts, profile.public_key, hvalue,"token")

      var x = Post("http://localhost:5001/profiles", tempProfile)
      println(x)
      val response: Future[HttpResponse] = pipeline(x)
      response.onComplete {
        case Success(response) =>
          println("response is " +response + response.status.toString.substring(0, 3).toLong)
          failedAttempts = 0
          myHiddenProfile = tempProfile
          getToken()
          self ! GetProfile
        case Failure(error) =>
          println(error, "failed to get profile")
          if (failedAttempts == 5)
          context.stop(self)
        else failedAttempts += 1
      }

    /**
     * AddPage is used to add new page to the server. Similar to profile this also doesn't
     * require authentication token.
     */
    case AddPage(page:Page) =>
      val hvalue:String = AESEncryptor.encrypt(symmetricKey,initVector,page)
      val tempPage: PageWrapper = new PageWrapper(page.id,page.username,page.likes,page.albums, page.photos, page.userposts, hvalue,"token")
      val response: Future[PageWrapper] = pagePipeline(Post("http://localhost:5001/pages" , tempPage))
      response.onComplete {
        case Success(response) =>
          failedAttempts = 0
          myHiddenPage = tempPage
          getToken()
        case Failure(error) =>
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
      }

    /**
     * AddPhoto: Adds encrypted photo
     */
    case AddPhoto(photo:Photo) =>
      val token:String = "Token"
      val hvalue:String = AESEncryptor.encrypt(symmetricKey,initVector,photo)
      val tempPhoto: PhotoWrapper = new PhotoWrapper(photo.id, photo.byPage,photo.from,photo.album,photo.can_delete,hvalue,token)
      val response: Future[PhotoWrapper] = photoPipeline(Post("http://localhost:5001/photos", tempPhoto))
      response.onComplete {
        case Success(response) =>
          //println("Added Post " + response)
          myHiddenPhoto = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, AddPhoto(photo:Photo))
      }

    /**
     * AddPost: Adds encrypted post
     */
    case AddPost(post:UserPost) =>
      val token:String = "Token"
      val hvalue:String = AESEncryptor.encrypt(symmetricKey,initVector,post)
      val tempPost: UserPostWrapper = new UserPostWrapper(post.id, post.byPage,post.from,post.privacy,hvalue,token)
      val response: Future[UserPostWrapper] = postPipeline(Post("http://localhost:5001/posts", tempPost))
      response.onComplete {
        case Success(response) =>
          //println("Added Post " + response)
          myHiddenPost = response
          failedAttempts = 0
        case Failure(error) =>
          ////println(error, "failed to get profile")
          if (failedAttempts == 5)
            context.stop(self)
          else failedAttempts += 1
          context.system.scheduler.scheduleOnce(999 milliseconds, self, AddPost(post:UserPost))
      }

    /**
     * This function acts as first step in making someone as new friend.
     * Step 1. Get their public key
     * Step 2. Encrypt your own symmetric key with their public key and
     * put in their pending request queue
     * Authentication Token: Not needed
      */
    case SendFriendRequest(id:Int) =>
      //Encrypt Symmetric key
      val publicKey = tokenPipeline(Get("http://localhost:5001/getUserPublicKey/"+id)).onComplete{
        case Success(auth) =>
          var key = RSAEncryptor.getPublicKeyFromString(auth.token)
          var fromKey = RSAEncryptor.encrypt(symmetricKey, key)
          println("From Key: "+ fromKey)
          self ! PostRequestToServer(id, fromKey)
        case Failure(err) =>
          println("Error while sending request"+err)
          self ! SendFriendRequest(id)
      }

    /**
     * This is the second step of sending someone friend request here
     * actor sends encrypted symmetric key to server, server puts this
     * key into pending request queue of the desired friend
     * Authentication Token: Required
     */
    case PostRequestToServer(id:Int, fromKey:String)=>
      println( userID +" is following from "+ id )
      var obj = new FriendRequest(userID, id, fromKey, tokenToSend.token)
      val response = pipeline(Put("http://localhost:5001/friendRequest", obj)).onComplete{
        case Success(response)=>
          println("response is " + response + response.status.toString.substring(0, 3).toLong)
          getToken()
        case Failure(err) =>
          println("Error while sending friend Request")
          getToken()
      }

    /**
     * GetPendingRequest message will ask actor to go to server
     * and fetch pending request queue of itself
     * This requires authentication token inorder to prove its own identity
     * Authentication Token: Required
     */
    case "GetPendingRequests" =>
      println("Getting pending requests for "+userID + tokenToSend)
      val response = friendPipeline(Get("http://localhost:5001/friendRequest/"+tokenToSend.id, tokenToSend)).onComplete{
        case Success(frndList) =>
          pendingList = frndList
          println("Pending request "+ pendingList)
          getToken()
        case Failure(err) =>
          println("Error occured while getting pending requests " + err)
          getToken()
      }
    /**
      * ProcessRequest message will make actor to process 1st friend
     * request from the queue. This is normally is the next step
     * after the GetPendingRequest.
     * Authentication Token: Not needed
     */
    case "ProcessRequest" =>
      if (pendingList.list.nonEmpty){
          var idTo:Int = pendingList.list(0)
        var keyGoingToMyStorage:String = pendingList.keys(0)
        self ! GetFriendsPublicKeyBeforeConfirming(idTo, keyGoingToMyStorage)
      }

    /**
     * Before confirming friend request from someone we need to get their public
     * so that actor can encrypt its own symmetric with their public to store
     * in friends storage.
     * Authentication Token: Not needed
     */
    case GetFriendsPublicKeyBeforeConfirming(id:Int, keyGoingToMyStorage:String) =>
      tokenPipeline(Get("http://localhost:5001/getUserPublicKey/"+id)).onComplete {
        case Success(auth) =>
          println("Successfully retrieved frnds public key"+auth)
          var key = RSAEncryptor.getPublicKeyFromString(auth.token)
          var keyGoingToFrndsStorage = RSAEncryptor.encrypt(symmetricKey, key)
          var obj = new ConfirmFriendRequest(userID, id, keyGoingToFrndsStorage, keyGoingToMyStorage, tokenToSend.token)
          self ! ConfirmRequest(obj)
        case Failure(err) =>
          println("Error in GetFriendsPublicKeyBeforeConfirming "+err)
          getToken()
      }

    /**
     * ConfirmRequest will confirm the friend request between two actors by sending
     * server information about the symmetric keys
     * Authentication Token: Required
     */
    case ConfirmRequest(obj:ConfirmFriendRequest) =>
      pipeline(Put("http://localhost:5001/confirmfriendRequest",obj)).onComplete{
        case Success(response) =>
          println("Successfully added frnd "+response)
          getToken()
          self ! GetFriends
        case Failure(err) =>
          println("Error adding frnd")
          getToken()
      }

    /**
     * GetProfile gets profile related to this particular actor.
     * After getting the profile decrypts it with symmetric key.
     * Authentication Token: Not needed
     */
    case GetProfile =>
      val response: Future[ProfileWrapper] = profilePipeline(Get("http://localhost:5001/profiles/"+userID))
      response.onComplete{
        case Success(profile) =>
          myHiddenProfile = profile
          myProfile = AESEncryptor.decrypt(symmetricKey, initVector, profile.hiddenValue).asInstanceOf[Profile]
          failedAttempts = 0
        case Failure(error) =>
          if (failedAttempts == 5) {
              failedAttempts += 1
            context.system.scheduler.scheduleOnce(999 milliseconds, self, GetProfile)
          }
      }
    //context.system.scheduler.scheduleOnce(9999 milliseconds, self, "DoSomethingNew")

    /**
     * GetFriendProfile: This can get any profile that is available on server. Though Decryption
     * logic only works for friends hence we get contents of friends through this method.
     * Authentication Token: Not needed
     */
    case GetFriendProfile(userID:Int) =>
      val response: Future[ProfileWrapper] = profilePipeline(Get("http://localhost:5001/profiles/"+userID))
      response.onComplete{
        case Success(profile) =>
          //Add logic to decrypt with the available symmetric key from friend list
          if (myFriendList.list.contains(profile.id)) {
            var encryptedKey = myFriendList.keys(myFriendList.list.indexOf(profile.id))
            var plainKey:String = RSAEncryptor.decrypt(Base64.decodeBase64(encryptedKey), key.getPrivate).asInstanceOf[String]
            var frndsProfile = AESEncryptor.decrypt(plainKey, initVector, profile.hiddenValue).asInstanceOf[Profile]
          }
          failedAttempts = 0
          getToken()
        case Failure(error) =>
          getToken()
      }

    /**
     *
     */
    case GetPhoto(photoID) =>
      val response: Future[PhotoWrapper] = photoPipeline(Get("http://localhost:5001/photos/"+photoID))
      response.onComplete{
        case Success(photo) =>
          myPhoto = AESEncryptor.decrypt(symmetricKey, initVector, photo.hiddenValue).asInstanceOf[Photo]
          failedAttempts = 0
        case Failure(error) =>
          println("Error while getting photo " + error)
      }

    /**
     *
     */
    case GetAlbum(albumID) =>
      val response: Future[Album] = albumPipeline(Get("http://localhost:5001/albums/"+albumID))
      response.onComplete{
        case Success(album) =>
          myAlbum = album
          failedAttempts = 0
        case Failure(error) =>
          println("Error while getting Album "+error)
      }

    /**
     *
     */
    case GetFriends =>
      val response: Future[FriendList] = friendPipeline(Get("http://localhost:5001/friendLists/"+userID))
      response.onComplete{
        case Success(frnds) =>
          myFriendList = frnds
          println("Successfully downloaded frnds list "+frnds)
          failedAttempts = 0
        case Failure(error) =>
          println("Error while getting friend list "+error)
      }

    /**
     * Ensure that you always get friend list before requesting any post from server.
     */
    case GetPost =>
      val response: Future[UserPostWrapper] = postPipeline(Get("http://localhost:5001/posts/"+name))
      response.onComplete{
        case Success(post) =>
          //TODO Decrypt with post.from's symmetric key which is present in friend List
          if (post.from != userID && post.privacy.equals("Friends")){
            if (myFriendList.list.contains(post.from)){
              var encryptedKey = myFriendList.keys(myFriendList.list.indexOf(post.from))
              var plainKey = RSAEncryptor.decrypt(Base64.decodeBase64(encryptedKey), key.getPrivate).asInstanceOf[String]
              myPost = AESEncryptor.decrypt(plainKey, initVector, post.hiddenValue).asInstanceOf[UserPost]
            }
          } else if (post.from == userID) {
            myPost = AESEncryptor.decrypt(symmetricKey, initVector, post.hiddenValue).asInstanceOf[UserPost]
          } else if (post.from != userID && post.privacy.equals("Custom")){
            //Decrypt with private key of self and public key of sender...
          }
          failedAttempts = 0
        case Failure(error) =>
          println("Error while getting user post " + error)
      }

    /**
     *
     */
    case GetPage =>
      val response: Future[PageWrapper] = pagePipeline(Get("http://localhost:5001/pages/"+name))
      response.onComplete{
        case Success(page) =>
          myPage = AESEncryptor.decrypt(symmetricKey, initVector, page.hiddenValue).asInstanceOf[Page]
          failedAttempts = 0
        case Failure(error) =>
          println("Error while getting page " + error)
      }

    case "DoSomethingNew" =>
      val noOfChoices = 2
      var choice = Random.nextInt(noOfChoices)
      println("Token for this transaction "+tokenToSend.token)
      choice match {
        case 0 =>
          self ! "GetPendingRequests"
          context.system.scheduler.scheduleOnce(9999 milliseconds, self, "DoSomethingNew")
        case 1 =>
          if (pendingList.list.nonEmpty){
            var idTo:Int = pendingList.list(0)
            var keyGoingToMyStorage:String = pendingList.keys(0)
            self ! GetFriendsPublicKeyBeforeConfirming(idTo, keyGoingToMyStorage)
          } else {
            var id = userID - 1
            if (id < 0)
              id = 1
            self ! SendFriendRequest(id)
            context.system.scheduler.scheduleOnce(9999 milliseconds, self, "DoSomethingNew")
          }
      }
  }

  override implicit def json4sFormats: Formats = DefaultFormats
}
