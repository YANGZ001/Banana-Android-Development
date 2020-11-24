# Banana-Android-Development

## What does this repository have for you?

This Repository stores the complete code for my "Semi-Netease Music" android development project. I finished this music application in 20 days, in order to sharpen my java programming skill and also learn the basic of Android frameworks.

In this project, I used the following components of Android frameworks:

- Activity: Of course, since every app has a MainActivity.class. And activity is also used to pass on some information.
- Content Provider: the Content provider component is used to look up music resources in android system. When android system is boot up, all the resources will be scanned and the information will be stored at a .xml file.
- Broadcast Receiver: the notification bar is just a remoteView and have no real java context. In order for the input events on notification bar to be responded, the bottom on the notification bar will send out some broadcasts when it is clicked. Meanwhile, the broadcast receiver in the Service will receive the message and act accordingly.
- Service: the play music service. Every action concerning the music player will ultimately be realized in the service.

And other components (highlights):

- AsyncTask: the AsyncTask interface is used to automatically open a new thread, which will run outside the MainThread and is used to perform some time-consuming tasks. So those long-time jobs would not congest the UI thread to cause lagging or ANR. 
- Timer: A small but really useful component, which is used to periodically update the UI.
- BaseAdatper: combined with ListView, the baseAdapter could customized to display music items, which contains album picture, name, duration and artist.
- BindService: the main difference between startService() and bindService() method is that startService() is independent of the caller, while bindService() requires service binding with the caller process. When the caller process is killed, bindService will call onUnBind() -> onDestroy() to vanish as well. In contrast, the service which stems from startService() will still exist in the memory and will not be killed unless stopService() is called.
- SharedPreferences: this component is used to store state when the app exits and load state when the app is opened again.

## What function or feature does this application have?

Basic functions of a music app:

- play
- pause
- play next
- play previous
- show music list
- click to play
- welcome page
- notification bar
- progress bar
- music volume control and alert when too load
- play mode control
- store and load play state when 

## How does the application work?

![Design of my Music application](E:\codes\Banana-Android-Development\音乐播放器.png)

Click [here][https://www.processon.com/view/link/5f957b8407912906db3e453a] just in case the picture does not show up.

## How to clone this project?

- Clone this project on github.com
- or download the ```testanr.zip``` file

## Why does this repository have such a weird name?

A good name makes your friends to remember you easier. 



This repository is only for Yang Zhang 's personal use.  