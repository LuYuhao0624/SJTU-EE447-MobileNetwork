# Lab 1: Android Programming

Dear Professors, TAs and possibly other visitors, welcome!

The first lab is basically an introduction to Android programming. Among the four provided simple applications, I choose to modify the *DrawLineSample* one to develop a simple Paint application with a few more features. It is more a reconstruction than a modification from my perspective because I start a new project rather than modify on the original project, which would otherwise take too much work. However, the previous draw line logic is pretty much kept.

The whole drawing module is wrapped in the class `PaintView`. It contains three basic functions listed below and is described in detail later.

1. Casual mode
2. Straight mode (normal/intelligent)
3. Eraser mode

*The project is debugged both on an API-28 virtual device and an Android-version-9 phone.*

## Requirements

- SDK version &ge; 28​
- API level &ge; 9​

## Main Activity

The blank in the middle is where the user draws. The three buttons (C, S, E) below corresponds to the previously mentioned three modes. The user can change to different modes by pressing the buttons. Moreover, if it is in straight mode, the user can tap the button again to flip between normal/intelligent mode. The text on its button switches between S/IS. Default is normal sub-mode. When the user switches to straight mode, it will stays the same sub-mode as the last time, e.g. a user is in straight (intelligent) mode and then switches to either eraser or casual mode, it will still be straight (intelligent) mode when he switches back to straight mode. In addition, the user can long click the E button to clear the canvas (no matter in which mode).

## Features

### Casual Mode

The user can draw a curve following the finger movement on screen. Start when touch the screen and stop when release.

This is derived from the original example except that the path is wrapped in a `Path` object rather than directly drawn on the bitmap. The canvas is updated to draw the path every touch event is detected. The complete logic is described below in terms of three different touch events

- `ACTION_DOWN`:
    1. Create a new `Path` object and set its start point to the coordinate of the event.
- `ACTION_MOVE`:
    1. Draw a line from the previous end point to the new end point.
- `ACTION_UP`:
    1. Draw the path on the bitmap and show on the canvas.

### Straight Mode (normal)

The user can draw a straight line in this mode. The start point is located where the user touch the screen. There is a line between the start point and the current position of user finger when moving for preview. The final straight line starts at the start point and ends where the user release the screen. Key of this part is to ensure only the latest line is drawn on the canvas, not all the lines for preview.

Likewise, the path is stored in a `Path` object. To implement the above logic, in case of different touch events, we do

- `ACTION_DOWN`: 
    1. Store the touch down coordinate as this will be start point for all following lines including previewing ones and the final one. 
- `ACTION_MOVE`:
    1. Reset the `Path` object if not reset before to prevent interference between previews. 
    2. Set the start point of the path to be the touch down coordinate recorded in the first step.
    3. Set the end point of the path to be the coordinate of current event.
    4. Create a temporary bitmap which is a shallow copy of the current bitmap. Note that the path has not been drawn on the current bitmap, thus the bitmap is the same as the one when `ACTION_DOWN` takes place.
    5. Draw the temporary bitmap on the canvas.
- `ACTION_UP`:
    1. Draw the path on the bitmap (not the temporary one) and present on canvas.

### Straight Mode (intelligent)

On the basis of normal mode, the start and end points of a straight line can be attached to the start or end point of an existing straight line (no matter this line is drawn in normal or intelligent mode) if the start or end point of the new line is near an existing one (if there are multiple ones then the nearest is chosen). And this applies to those lines in preview as well. In order to keep a record of all existing start and end points, we maintain a list of them in `end_points`. Difference is highlighted in italic.

- `ACTION_DOWN`:
1. Store the touch down coordinate to be the start point.
    2. - *If there exists a start or end point within a certain range of the touch down point, we change the coordinate of the start point to be the coordinate of the nearest existing start or end point.*
        - *Otherwise, store the coordinate of this touch down event and add it to `end_points`.*
    
- `ACTION_MOVE`
    1. Reset the `Path` object if not reset before to prevent interference between previews. 

    2. *Set the starting point of the path according to the coordinate in `ACTION_DOWN`.*

    3. - *If there exists a start or end point within a certain range of the touch down point, we set the coordinate of the end point to be the coordinate of the nearest one.*

        - *Otherwise, set the coordinate of the end point to be the coordinate of the current event.*

    4. Create a temporary bitmap which is a shallow copy of the current bitmap. Note that the path has not been drawn on the current bitmap, thus the bitmap is the same as the one when `ACTION_DOWN` takes place.

    5. Draw the temporary bitmap on the canvas.

- `ACTION_UP`:

    1. Draw the path on the bitmap (not the temporary one) and present on canvas.
    2. *If there does not exist a start or end point within the range of the new end point, add this new one to `end_points`.*

Notice that the range is set to be a circle with a $50$-pixel radius (defined in the constant `RADIUS_SQUARE` in class `PaintView`) which performs good in both of my $2560\times1440$ devices. This might look strange in other device with a much smaller or larger resolution. It can be changed to a fraction of the screen resolution for generalization.

### Eraser Mode

The logic of this mode is exactly the same with casual mode. The only difference is the paint. Paint in eraser mode is thicker and white (can be set to the color of background to adapt different background color).

Moreover, there is a clear all function, which is implemented by letting the canvas draw white (the background color) on the whole bitmap.

