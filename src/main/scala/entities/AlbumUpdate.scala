package entities

/**
 * Created by sahilpt on 11/30/15.
 */
case class AlbumUpdate( id:Option[Int] = None,
                  from:Option[Int] = None,
                  count:Option[Int] = None,
                  description:Option[String] = None,
                  can_upload:Option[Boolean] = None,
                  privacy:Option[String] = None,
                  photos: Option[List[Int]] = None)
