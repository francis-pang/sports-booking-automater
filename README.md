# Project Name – Sports Booking Automater
## Project Initiation 
The thought for the product began when one day my friend told me this problem he had with booking a volleyball court in the Singapore Sports Hub. He wanted to book the outdoor free volleyball court on the weekend. However, the online booking website only allows him to book up to the upcoming last week day, that is he can only book till next Friday. And the booking slots on the weekend only open at 12 midnight on Saturday. Hence he is unable to make it online to make the booking for the time slot he wanted. He came up with a solution to that problem. He would build a program to book the court for him automatically when the booking slots for the next weekend open. Hence he came and we discussed how he could do it. 
I gave him some help here and there, before he engaged in the program. Finishing it, he has an issue passing the program to his friends as he had written it in python, and it is hard for him to extend the program to his friend. He came to me and I suggest how about I help him to pack it into a Java Archive (JAR). I helped in the morning. In the end, I got hooked into it, and I came up with more ideas to further enhance this thought of the booking automation. 
## Concept/Feature Analysis 
### Scheduled booking
This application will be able to run scheduled such that it will be run precisely at the time when the slot open. This will increase the chance of booking the court which the user desires. 
### Consecutive hours booking
A booking slot of the facility is one hour by default. If the user wants to play for 2 consecutive hours, the application will be able to look for 2 consecutive empty slots for booking, instead of booking one at a time, which might not be consecutive. 
### Restricted time range
The user can put in multiple time range they want to book the court for. For example, you can put in such time: 23-October-2016 07:00am – 10:00am, then 23-October-2016 02:00 – 05:00pm. With this, you can indicate your availability in advanced.
### Booking on the fly 
If the user wants to book the court on fly, and his information is pre-configured in the program. All he needs to do is to select the court he wants and he can book it immediately. This might not be such a strong feature because that is what you can do in the Sports Hub booking website as well.

![Interaction between Actor, Sports Bookng Automater & Sports Hub Booking System](https://s15.postimg.org/kj1tphe23/Class_Diagram1_Booking_Automater.png)
