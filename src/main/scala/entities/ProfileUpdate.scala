package entities

/**
 * Created by gokul on 11/29/15.
 */
case class ProfileUpdate(birthday:Option[String] = None, 
                         email:Option[List[String]] = None, 
                         first_name:Option[String] = None, 
                         gender:Option[String] = None,
                         last_name:Option[String] = None, 
                         public_key:Option[String] = None, 
                         albums:Option[List[Int]] = None,
                         photos:Option[List[Int]] = None,
                         likedpages:Option[List[Int]] = None,
                         userposts:Option[List[Int]] = None,
                         token:String)
