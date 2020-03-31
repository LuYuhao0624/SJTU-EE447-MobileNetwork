# Lab 1: Android Programming

Dear Professors, TAs and possibly other visitors, welcome!

The first lab is basically an introduction to Android programming. Among the four provided simple applications, I choose to modify the *DrawLineSample* one to develop a simple Paint application with a few more features. It is more a reconstruction than a modification from my perspective because I start a new project rather than modify on the original project, which would otherwise take too much work. However, the previous draw line logic is pretty much kept.

*The project is debugged both on an API 28 virtual device and an Android-version-9 phone.*

## Requirements

- SDK version $\ge28$
- API level $\ge9$

## Features

### Casual Drawing

The user can draw a curve following the finger movement on screen. Start when touch the screen and stop when release.

This is derived from the original example except that the path is wrapped in a `Path` object rather than directly drawn on the bitmap.



