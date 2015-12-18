package entities

/**
 * Created by sahilpt on 12/17/15.
 */
case class UserPostWrapper(id:Int,  /*identity of the content*/
                           byPage:Int,
                           from:Int,/*identity of the user who posted the content*/
                           //caption:String, /*caption string*/
                           //object_id: String, /*id of the photo or video uploaded in the post*/
                           //message:String,
                           privacy:String,
                           //sharedWith:List[Int],
                           hiddenValue:String,
                           token:String)