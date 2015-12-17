package entities

/**
 * Created by gokul on 11/29/15.
 */
case class PageUpdate(//id:Int,
                      access_token:Option[String] = None,
                      can_checkin:Option[Int] = None,
                      can_post:Option[Int] = None,
                      email:Option[List[String]] = None,
                      username:Option[String] = None,
                      likes:Option[Long] = None,
                      albums:Option[List[Int]] = None,
                      photos:Option[List[Int]] = None,
                      userposts: Option[List[Int]] = None,
                      token:String)
