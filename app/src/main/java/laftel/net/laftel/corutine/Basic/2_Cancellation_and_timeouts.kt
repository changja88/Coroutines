import kotlinx.coroutines.experimental.*


// 1. cancel
fun main1(args: Array<String>) = runBlocking {
    val job: Job = launch {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancel() // cancels the job
    job.join() // waits for job's completion
    println("main: Now I can quit.")
}
// - job을 정지 시킨다
// - launch를 시키면 결과 값으로 Job이 나온다


// 2. cancelAndJoin
fun main2(args: Array<String>) = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (i < 50) { // computation loop, just wastes CPU
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
}
// - 이거 존나 이상함 cancel을 시키고 다시 join을 시키는 건데 다시 join을 시키면
//   취소한 지점부터 다시 시작 하게 된다 이럴거면 왜 cancel 을 시킴?

// 3. isActive
fun main3(args: Array<String>) = runBlocking {
    val startTime = System.currentTimeMillis()
    val job = launch(Dispatchers.Default) {
        var nextPrintTime = startTime
        var i = 0
        while (isActive) { // cancellable computation loop
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                println("I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
}
// - isActive 은 코루틴 스코프 객체 안에 있는 extension property 이다
// - 이걸 T/F 체크를 해서 Job을 정지 시킬 수 있다

// 4. finally
fun main4(args: Array<String>) = runBlocking {
    val job = launch {
        try {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            println("I'm running finally")
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
}
// - Job이 정지 될때 finally 블럭이 돌고 정지가 된다
// - finally 블럭 안에서 suspending 함수를 사용하게 되면 CancellationException이 발생한다

// 5. NonCancellable
fun main5(args: Array<String>) = runBlocking {
    val job = launch {
        try {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        } finally {
            withContext(NonCancellable) {
                println("I'm running finally")
                delay(1000L)
                println("And I've just delayed for 1 sec because I'm non-cancellable")
            }
        }
    }
    delay(1300L) // delay a bit
    println("main: I'm tired of waiting!")
    job.cancelAndJoin() // cancels the job and waits for its completion
    println("main: Now I can quit.")
}
// - finally 블럭 아네서 suspending 함수를 사용하고 싶으면 NonCancellable을 이용해야 한다

// 6. Tiemout
fun main6(args: Array<String>) = runBlocking {
    withTimeout(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
    }
}
// - timeout 안에 작업이 끝나지 않으면 TimeourCancellationException이 발생한다

// 7. TimeoutOrNull
fun main(args: Array<String>) = runBlocking {
    val result = withTimeoutOrNull(1300L) {
        repeat(1000) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }
        "Done" // will get cancelled before it produces this result
    }
    println("Result is $result")
}
// - 타임 아웃 시간안에 작업을 다하지 못하면 eception 이 발생하는 것이 아니라 null 이 나온다


