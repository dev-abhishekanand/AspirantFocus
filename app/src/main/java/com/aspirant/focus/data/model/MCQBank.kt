package com.aspirant.focus.data.model

object MCQBank {
    val questions = listOf(
        MCQQuestion(
            id = 1,
            question = "Which law states that induced EMF is directly proportional to the rate of change of magnetic flux?",
            options = listOf("Ohm's Law", "Faraday's Law", "Coulomb's Law", "Ampere's Law"),
            correctIndex = 1,
            subject = "Physics"
        ),
        MCQQuestion(
            id = 2,
            question = "Which constitutional amendment added Fundamental Duties to the Indian Constitution?",
            options = listOf("42nd Amendment (1976)", "44th Amendment (1978)", "73rd Amendment (1992)", "86th Amendment (2002)"),
            correctIndex = 0,
            subject = "Polity / GS"
        ),
        MCQQuestion(
            id = 3,
            question = "What is the oxidation state of Chromium in potassium dichromate (K2Cr2O7)?",
            options = listOf("+3", "+4", "+6", "+7"),
            correctIndex = 2,
            subject = "Chemistry"
        ),
        MCQQuestion(
            id = 4,
            question = "If f(x) = sin(x) + cos(x), what is the derivative value f'(0)?",
            options = listOf("0", "1", "-1", "2"),
            correctIndex = 1,
            subject = "Mathematics"
        ),
        MCQQuestion(
            id = 5,
            question = "Which hormone is primarily responsible for triggering the rapid 'fight or flight' response?",
            options = listOf("Insulin", "Adrenaline (Epinephrine)", "Thyroxine", "Melatonin"),
            correctIndex = 1,
            subject = "Biology"
        )
    )
}
