package entities

/**
 * Created by sahilpt on 11/30/15.
 */
case class PhotoWrapper (id:Int,
                         byPage:Int,  /* 1 indicates photo is posted by page otherwise user*/
                         from:Int, /*user identity*/
                         album:Int,
                         can_delete:Boolean,
                         hiddenValue:String,
                         token:String)
