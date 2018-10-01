package laftel.net.laftel.corutine.Basic

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import kotlin.system.measureTimeMillis


suspend fun CoroutineScope.massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        val jobs = List(n) {
            launch {
                repeat(k) { action() }
            }
        }
        jobs.forEach { it.join() }
    }
    println("Completed ${n * k} actions in $time ms")
}

var counter = 0


fun main12(args: Array<String>) = runBlocking<Unit> {
    GlobalScope.massiveRun {
        counter++
    }
    println("Counter = $counter")
}
// 문제
// - 사방팔방에서 쓰레들이 counter를 가져다가 쓰니깐 제대로된 값이 안나온다 (10,000이 나와야함)

// 1. Volatiles
// -> @Volatile 어노테이션을 conter에 달아 준다
// - atomic 효과를 주지만 but do not provide atomicty of larger actions

// 2. AtomicInteger
// -> var counter = AtomicInteger()
// - 해결됨

// 4. Thread confinement fine-grained
val counterContext = newSingleThreadContext("CounterContext")

fun main13(args: Array<String>) = runBlocking<Unit> {
    GlobalScope.massiveRun {
        // run each coroutine with DefaultDispathcer
        withContext(counterContext) {
            // but confine each increment to the single-threaded context
            counter++
        }
    }
    println("Counter = $counter")
}
// - 쓰레드 하나를 딱 정해줘서 하면 느려지지만 문제는 해결됨

// 5. Mutual exclusion
val mutex = Mutex()

fun main14(args: Array<String>) = runBlocking<Unit> {
    GlobalScope.massiveRun {
        mutex.withLock {
            counter++
        }
    }
    println("Counter = $counter")
}
// - Mutex 는 lock(), unlock()가 있고 이걸로 -> never executed concurrently하게 한다
//   하지만 lock, unlock은 suspending function(not block thread)이기 때문에 4번보다 쫌 빠르다

// 6. Actors
sealed class CounterMsg

object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply

// This function launches a new counter actor
fun CoroutineScope.counterActor() = actor<CounterMsg> {
    var counter = 0 // actor state
    for (msg in channel) { // iterate over incoming messages
        when (msg) {
            is IncCounter -> counter++
            is GetCounter -> msg.response.complete(counter)
        }
    }
}

fun main(args: Array<String>) = runBlocking<Unit> {
    val counter = counterActor() // create the actor
    GlobalScope.massiveRun {
        counter.send(IncCounter)
    }
    // send a message to get a counter value from an actor
    val response = CompletableDeferred<Int>()
    counter.send(GetCounter(response))
    println("Counter = ${response.await()}")
    counter.close() // shutdown the actor
}
// -
