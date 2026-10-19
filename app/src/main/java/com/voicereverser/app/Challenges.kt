package com.voicereverser.app

class Challenge(val phrase: String, val level: String)

object Challenges {
    val list = listOf(
        Challenge("الصبر مفتاح الفرج", "سهل"),
        Challenge("الوقت من ذهب", "سهل"),
        Challenge("الكلمة الطيبة صدقة", "سهل"),
        Challenge("مرحبا شلونكم اليوم", "سهل"),
        Challenge("جار قبل الدار والرفيق قبل الطريق", "متوسط"),
        Challenge("اللي ما يعرف الصقر يشويه", "متوسط"),
        Challenge("القافلة تسير والكلاب تنبح", "متوسط"),
        Challenge("She sells seashells by the seashore", "متوسط"),
        Challenge("خياط خاط خيط خياطة", "صعب"),
        Challenge("سيدي سعيد سأل سلمى عن سالم", "صعب"),
        Challenge("ثلاثة وثلاثون ثورا ثارت ثورتها", "صعب"),
        Challenge("How much wood would a woodchuck chuck", "صعب")
    )
}
