package entities

/**
 * Created by sahilpt on 11/30/15.
 */
case class Photo (
                        id:Int,   /* unique photo ID */
                        from:Int, /*user identity*/
                        link:String,
                        name:String, /*name of the photo*/
                        album:Int,   /* album_id of the album photo belongs to (-1) in case if it is individual photo */
                        can_delete:Boolean)
