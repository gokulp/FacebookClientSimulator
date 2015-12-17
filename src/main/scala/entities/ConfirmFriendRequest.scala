package entities

/**
 * Created by gokul on 11/29/15.
 */
case class ConfirmFriendRequest( idFrom:Int,
                                 idTo:Int,
                                 keyFrom:String,
                                 keyTo:String,
                                 token:String);