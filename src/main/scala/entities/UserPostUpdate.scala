package entities

/**
 * Created by gokul on 11/29/15.
 */
case class UserPostUpdate( from:Int,
                           caption:Option[String],
                           object_id: Option[String],
                           message:Option[String],
                           privacy:Option[String],
                           sharedWith:Option[List[Int]])