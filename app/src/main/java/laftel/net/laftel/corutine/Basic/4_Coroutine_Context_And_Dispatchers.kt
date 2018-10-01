package laftel.net.laftel.corutine.Basic

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext


// 1. Dispatcher
fun main111(args: Array<String>) = runBlocking<Unit> {
    launch {
        // context of the parent, main runBlocking coroutine
        println("main runBlocking      : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(Dispatchers.Unconfined) {
        // not confined -- will work with main thread
        println("Unconfined            : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(Dispatchers.Default) {
        // will get dispatched to DefaultDispatcher
        println("Default               : I'm working in thread ${Thread.currentThread().name}")
    }
    launch(newSingleThreadContext("MyOwnThread")) {
        // will get its own new thread
        println("newSingleThreadContext: I'm working in thread ${Thread.currentThread().name}")
    }
}
// Unconfined - 쓰지마 그냥
// Default - GlobalScope.luanch{}와 동일하다 -> 백그라운드 코루틴 스코프에서 돈다
// newSingleThreadContext - 새로운 코루틴 스코프를 만들어서 돈다
// without parameter - 런치 된 곳의 코루틴 스코프안에서 돈다

// 2. withContext
fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

fun main222(args: Array<String>) {
    newSingleThreadContext("Ctx1").use { ctx1 ->
        newSingleThreadContext("Ctx2").use { ctx2 ->
            runBlocking(ctx1) {
                log("Started in ctx1")
                withContext(ctx2) {
                    log("Working in ctx2")
                }
                log("Back to ctx1")
            }
        }
    }
}
// - withContext 를 사용 하여 쓰레드를 왔다 갔다 할 수 있다

// 3. Childeren of a coroutine
fun main333(args: Array<String>) = runBlocking<Unit> {
    // launch a coroutine to process some kind of incoming request
    val request = launch {
        // it spawns two other jobs, one with GlobalScope
        GlobalScope.launch {
            println("job1: I run in GlobalScope and execute independently!")
            delay(1000)
            println("job1: I am not affected by cancellation of the request")
        }
        // and the other inherits the parent context
        launch {
            delay(100)
            println("job2: I am a child of the request coroutine")
            delay(1000)
            println("job2: I will not execute this line if my parent request is cancelled")
        }
    }
    delay(500)
    request.cancel() // cancel processing of the request
    delay(1000) // delay a second to see what happens
    println("main: Who has survived request cancellation?")
}
// - 부모 코루틴 스코프를 캔슬 시키면 자식 코루틴 스코프도 전부 멈추게 되는게 기본
// - 하지만 GlobalScope는 별개이다 부모 스코프가 캔슬 되도 영향을 받지 않는다

// 4. Parental responsibilities
fun main444(args: Array<String>) = runBlocking<Unit> {
    // launch a coroutine to process some kind of incoming request
    val request = launch {
        repeat(3) { i ->
            // launch a few children jobs
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                println("Coroutine $i is done")
            }
        }
        println("request: I'm done and I don't explicitly join my children that are still active")
    }
    request.join() // wait for completion of the request, including all its children
    println("Now processing of the request is complete")
}
// - 부모 스코프는 자식 쓰레드ㄷ가 완료 될때까지 기다린다
// - 부모 스코프는 선언적으로 자식 스코프를 launch시킬 필요 없다 알아서 launch가 된다
//   (Job.join) 을 안해도 알아서 자식 스코프가 launch 된다는 뜻

// 5. Naming coroutines for debugging

fun main555(args: Array<String>) = runBlocking(CoroutineName("main")) {
    log("Started main coroutine")
    // run two background value computations
    val v1 = async(CoroutineName("v1coroutine")) {
        delay(500)
        log("Computing v1")
        252
    }
    val v2 = async(CoroutineName("v2coroutine")) {
        delay(1000)
        log("Computing v2")
        6
    }
    log("The answer for v1 / v2 = ${v1.await() / v2.await()}")
}
// - CoroutineName을 이용하여 스코프에 이름을 지어 줄수 있다

// 6. Combining context elements
fun main666(args: Array<String>) = runBlocking<Unit> {
    launch(Dispatchers.Default + CoroutineName("test")) {
        println("I'm working in thread ${Thread.currentThread().name}")
    }
}
// - '+'operation을 사용 하여 복수개의 코루틴 옵션을 걸수 있다


// 7. Cancellation via explicit job -> 안드로이드에서 코루틴 사용법
class Activity : CoroutineScope {

    lateinit var job: Job

    fun create() {
        job = Job()
    }

    fun destroy() {
        job.cancel()
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun doSomething() {
        // launch ten coroutines for a demo, each working for a different time
        repeat(10) { i ->
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
    }
}

fun main777(args: Array<String>) = runBlocking<Unit> {
    val activity = Activity()
    activity.create() // create an activity
    activity.doSomething() // run test function
    println("Launched coroutines")
    delay(500L) // delay for half a second
    println("Destroying activity!")
    activity.destroy() // cancels all coroutines
    delay(1000) // visually confirm that they don't work
}
// - coroutineContext 를 오버라이드 해주면 엑티비티 안에서 코루틴 스코프를 정확히 말해주지 않아도 사용 할수 있게 된다
// 따라서, doSomething 에서 launch를 runBlocking으로 감싸지 안하도 사용할 수 있게 된다
// -


// 8. Thread-local data
val threadLocal = ThreadLocal<String?>() // declare thread-local variable

fun main(args: Array<String>) = runBlocking<Unit> {
    threadLocal.set("main")
    println(
        "Pre-main, current thread: ${Thread.currentThread()}," +
                " thread local value: '${threadLocal.get()}'"
    )

    val job = launch(Dispatchers.Default + threadLocal.asContextElement(value = "launch")) {
        println(
            "Launch start, current thread: ${Thread.currentThread()}, " +
                    "thread local value: '${threadLocal.get()}'"
        )
        yield()
        println(
            "After yield, current thread: ${Thread.currentThread()}, " +
                    "thread local value: '${threadLocal.get()}'"
        )
    }

    job.join()
    println(
        "Post-main, current thread: ${Thread.currentThread()}, " +
                "thread local value: '${threadLocal.get()}'"
    )
}
// - 코루틴 쓰레드들은 not bound to any particular thread, it is hard to achieve it manually
//   without writing a lot of boilerplate -> 이걸 해결 하자
// - ThreadLocal, asContextElement 를 사용 해서 위 문제를 해결 할 수 있다























