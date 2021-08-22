package com.shevelev.comics_viewer.ui.activities.view_comics.user_actions_managing

/**
 * State of view
 */
enum class ViewStateCodes {
    /** Size of mesh is equal to size of screen (mesh not zoomed)    */
    NotResized,
    /** Size of mesh is not equal to size of screen (mesh zoomed)    */
    Resized
}