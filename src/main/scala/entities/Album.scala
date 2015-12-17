package entities

/**
 * Created by sahilpt on 11/30/15.
 */
case class Album( id:Int, /*unique album_id*/
                  from:Int, /* user_id of album owner */
                  count:Int, /*Number of photos*/
                  description:String, /* Description of the album*/
                  can_upload:Boolean, /* if user can upload contents*/
                  privacy:String,
                  photos: List[Int]) /* if album is public or private*/