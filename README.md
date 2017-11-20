# gobible-creator

Source code for the **SymScroll** branch of **Go Bible Creator** the **Java SE** program for creating **Go Bible** Java ME apps.

For further details, visit https://crosswire.org/wiki/Projects:Go_Bible/SymScroll

NB. This repository does **not** include the source code for **GoBibleCore**.

**Go Bible** is a free Bible viewer application for Java mobile phones, originally developed by Jolon Faichney.

**Go Bible** was officially adopted in 2008 by the **CrossWire Bible Society**.

Following the rapid growth of _smart phones_, apps like **Go Bible** for _feature phones_ are now well past their peak popularity.

**Go Bible** is no longer being actively developed. 

## Introduction

GoBibleCore will create the core of an app for MIDP 2.0 cmpatible phones. It does not the Bible information per se, which is what you have to add using GoBibleCreator.
GoBibleCreator will take GoBibleCore's output jar file and add the selected Bible books to that file. To do this both of them follow a specific format which is explained at https://github.com/DavidHaslam/GoBibleCore.
The latest update includes section headings whih were not present before.

## Section headings

Since November 2017 GoBible apps can also show section headings. This is currently only supported for USFM file formats.

#### USFM
GBC will parse \s, \s1, \s2 tags and pass them as headings to GoBibleCore.
