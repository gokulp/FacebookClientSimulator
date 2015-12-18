import entities._

/**
 * Created by gokul on 11/29/15.
 */
case object InitializeServer
case object StartSimulation
case object PrintStatistics
case object StartSimulatingUserBehavior
case class initiate(noOfUsers:Int) ;
case object GetProfile
case class GetFriendProfile(userID:Int)
case object GetFriends
case object NewsFeed
case object initiate ;
case class GetPhoto(photoId:Int)
case class GetAlbum(albumID:Int)
case object GetPost
case object GetPage
case class UpdateProfile(profile:Profile)
case class UpdateAlbum(album:AlbumUpdate)
case class UpdateFriendList(friendList:FriendListUpdate)
case class UpdatePage(page:PageUpdate)
case class UpdatePhoto(photo:PhotoUpdate)
case class UpdatePost(post:UserPostUpdate)
case class DeleteProfile(profile:Profile)
case class DeletePhoto(photo:Photo)
case class DeleteUserPost(post:UserPost)
case class DeletePage(page:Page)
case class DeleteAlbum(album:Album)
case class DeleteFriend(userId: FriendListUpdate)
case class AddPost(post:UserPost)
case class AddAlbum(album:Album)
case class AddPhoto(photo:Photo)
case class AddPage(page:Page)
case class AddProfile(profile:Profile)
case class SendFriendRequest(id:Int)
case class PostRequestToServer(id:Int, fromKey:String)
case class GetFriendsPublicKeyBeforeConfirming(id:Int, idToKey:String)
case class ConfirmRequest(obj:ConfirmFriendRequest)
