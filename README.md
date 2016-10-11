# qapv-simulator
Automatically exported from code.google.com/p/qapv-simulator

# Introduction

This area hosts the source code repository for the QAPV Simulator.  This program is meant to fully simulate and expose the transactions and objects involved in adhering to the QAPV IHE-RO Profile.

The profile describes the automated behavior and handshakes between two "actors": a Quality Check Requester and a Quality Check Performer.  A Quality Check Requester is most likely fulfilled by a radiation treatment device.  It's action consists of asking, through the described transactions and objects, a Quality Check Performer to do a last moment check that a radiation plan is not egregiously dangerous.  The other responsibility it has is to veto the treatment if the Quality Check Performer finds the plan wanting.  The Quality Check Performer is likely a specialized implementation of a Radiation QA Device fine tuned to detect differences in the plan, or values far out of band in the plan.

<b> Intent </b>
  * Proof of concept:  we are learning a lot about the fitness of the profile based on implementation
  * Test Bed: the simulator is constructed so that one actor or the other can be fulfilled by a manufacturer's device.
  * Learning: the simulator tries to capture all the traffic, and so can show what DICOM traffic is required, or show its shortcomings according to the profile.


<b> Details </b>

This site contains:
  * Up to date source code
  * Link to download an installable java package

This implementation uses Jave to keep it platform independent, and uses DCM4CHE DICOM Java library to keep it open source and royalty free.

<b> Install </b>
Version 1.0 - Unfortunately this is targeted at windows.  The code is currently not careful with file delimiters etc., and the install cmd files are DOS specific.  If you need to run on another platform, grab the source and you'll have to modify the hardcoded paths, etc.
Download the QAPVBuild.zip, extract and run from a dos prompt.  From there run QAPVSIM\BIN\runqapvsim.cmd

<b> Execute </b>
The version runs well enough, but doing things out of order will lead to unpredictable results.  There are many OPPORTUNITIES for reducing the fragility of the code.  In general the apps expect you to follow the QAPV transaction order:
Quality Check Requester (QCR) / Quality Check Performer (QCP)
  * QCR Echo
  * QCR Create UPS
  * QCR Subscribe to UPS
  * QCP fetch workitem
  * QCP do quality check 
  * QCR get output sequence of UPS from QCP
  * QCR C-Move Quality Report 
  * QCR unsubscribe to UPS

<b> Help </b>
If you are starting to seriously use this simulator, you will find deficiencies.  Koua and Chris invite you to help in maintaining the code.  Request access via the email addresses on this site.  If the change is small, you can send one of us the code sample.  We use GIT as the repository client.

<b> History </b>
*2012-08-27*  Released a working copy, version 1.0.  Still very shameful error handling and some implementation details (such as content of the Quality Check Report) are clearly in error and need further changes.
