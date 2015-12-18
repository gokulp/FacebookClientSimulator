
package entities

/**
 * Created by sahilpt on 12/17/15.
 */
case class PageWrapper(id:Int, /*page ID*/
                       //access_token:String, /* access_token for the page - only possesed by the administrators */
//                       can_checkin:Int, /* can user checkin with this page */
//                       can_post:Int, /* can user post on this page */
                       //email:List[String], /* List of the email addresses of the administrators or organization */
                       username:String, /* name of the page */
                       likes:Long,
                       albums: List[Int],
                       photos: List[Int],
                       userposts: List[Int],  /* number of likes on the page */
                       hiddenValue:String,
                       token:String)