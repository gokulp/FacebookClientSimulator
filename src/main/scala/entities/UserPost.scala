package entities

/**
 * Created by gokul on 11/29/15.
 */
case class UserPost(id:Int,  /*identity of the content*/
                from:Int,/*identity of the user who posted the content*/
                caption:String, /*caption string*/
                object_id: String, /*id of the photo or video uploaded in the post*/
                message:String /* status message in the post */
                 )