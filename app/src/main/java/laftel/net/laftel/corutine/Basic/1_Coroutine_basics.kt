package laftel.net.laftel.corutine.Basic

import kotlinx.coroutines.experimental.*


// 1. GlobalScope
fun main0(args: Array<String>) {
    GlobalScope.launch {
        // launch new coroutine in background and continue
        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
        println("World!") // print after delay
    }
    println("Hello,") // main thread continues while coroutine is delayed
    Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive
}
// - 백그라운드 에서 도는 코루틴 스코프이다
// - 메인 쓰레디가 죽으면 백그라운드 쓰레드의 println을 보여 줄수 없음으로 맨 아래에서 쓰레드를 2초간 살려 놓는다

// 2. delay
fun main1(args: Array<String>) {
    GlobalScope.launch {
        // launch new coroutine in background and continue
        delay(1000L)
        println("World!")
    }
    println("Hello,") // main thread continues here immediately
    runBlocking {
        // but this expression blocks the main thread
        delay(2000L)  // ... while we delay for 2 seconds to keep JVM alive
    }
}
// - Thread.sleep()는 쓰레드를 blocking 하는 반면 delay() 는 쓰레드를 blocking 하지 않는다
//   따라서 runBlokcing으로 감싸야 Thread.sleep()과 같아진다

// 3. runBlocking<Unit>{}
fun main(args: Array<String>) = runBlocking {
    // start main coroutine
    GlobalScope.launch {
        // launch new coroutine in background and continue
        delay(1000L)
        println("World!")
    }
    println("Hello,") // main coroutine continues here immediately
    delay(2000L)      // delaying for 2 seconds to keep JVM alive
}
// - start the top-level main corutine.
// - Unit인 이유는 well-formed main function in Kotlin has to return Unit 이기 때문
// - 따라서 runBlocking은 메인쓰레드는 잡아두고 탑 레벨 코루틴을 연다

// 4. Job
fun main3(args: Array<String>) = runBlocking {
    val job = GlobalScope.launch {
        // launch new coroutine and keep a reference to its Job
        delay(1000L)
        println("World!")
    }
    println("Hello,")
    job.join() // wait until child coroutine completes
}
// - 열린 코루틴 스코프에 작업을 추가한다 -> 새로운 코루튼 스코프를 연
// - 코루틴 스코는 자식 스코프나 잡이 완료를 할때 까지 기다리기 때문에 delay()를 해주지 않아도 된다

// 5. launch
fun main4(args: Array<String>) = runBlocking {
    // this: CoroutineScope
    launch {
        // launch new coroutine in the scope of runBlocking
        delay(1000L)
        println("World!")
    }
    println("Hello,")
}
// - 4번 처럼 join으로 코루틴 스코프를 여는 방법도 있지만 launch로 여는 방법도 있다
// - launch 를 시키면 Job이 나온다

// 6. coroutinScope{}
fun main123123(args: Array<String>) = runBlocking { // this: CoroutineScope
    launch {
        delay(200L)
        println("Task from runBlocking")
    }

    coroutineScope { // Creates a new coroutine scope
        launch {
            delay(500L)
            println("Task from nested launch")
        }

        delay(100L)
        println("Task from coroutine scope") // This line will be printed before nested launch
    }

    println("Coroutine scope is over") // This line is not printed until nested launch completes
}
// - declare your on scrop using coroutineScope
// - runBlocking과 마찬가지로 launch된 자식들이 complete될때까지 종료 되지 않는다
// - runBlokcing과 다른 점은 runBlocking은 자식들이 완료되 될때까지 current 쓰레드를 블럭하지만
//   coroutineScope는 자식들이 완료될때까지 current쓰레드를 블럭지 않는다


// 7. Suspend
fun main6(args: Array<String>) = runBlocking {
    launch { doWorld() }
    println("Hello,")
}

suspend fun doWorld() {
    delay(1000L)
    println("World!")
}
// - 일반 함수와 똑같지만 추가 기능이 있다 -> delay()같은 다른 suspending 함수를 사용 할 수 있다





















































