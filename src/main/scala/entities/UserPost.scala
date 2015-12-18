package entities

/**
 * Created by gokul on 11/29/15.
 */
case class UserPost(id:Int,  /*identity of the content*/
                    byPage:Int,
                    from:Int,/*identity of the user who posted the content*/
                    caption:String, /*caption string*/
                    object_id: String, /*id of the photo or video uploaded in the post*/
                    message:String,
                    privacy:String,
                    sharedWith:List[Int])