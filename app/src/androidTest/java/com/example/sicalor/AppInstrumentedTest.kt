package com.example.sicalor

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.database
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.fail
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AppInstrumentedTest() {

    @Test
    fun testLoginShouldRespondUnder3Seconds() {
        val latch = CountDownLatch(1)
        var result = false

        val email = "chyperxvagos@gmail.com"
        val password = "akuganteng4"
        val startTime = System.currentTimeMillis()
        var duration: Long? = null

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                val elapsedTime = System.currentTimeMillis() - startTime
                duration = elapsedTime
                result = it.isSuccessful && elapsedTime < 3000
                latch.countDown()
            }

        latch.await()
        Log.d("Test", "Login response time: $duration ms")
        assertTrue("Login failed or took more than 3 seconds", result)
    }

    @Test
    fun testRegisterAndDeleteAfterShouldRespondUnder3Seconds() {
        val latch = CountDownLatch(1)
        var result = false
        var duration: Long? = null

        val email = "dummyuser${System.currentTimeMillis()}@test.com"
        val password = "test1234"
        val startTime = System.currentTimeMillis()

        val auth = FirebaseAuth.getInstance()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val elapsed = System.currentTimeMillis() - startTime
                result = elapsed < 3000
                duration = elapsed

                it.user?.delete()?.addOnCompleteListener {
                    latch.countDown()
                }
            }.addOnFailureListener {
                latch.countDown()
            }

        latch.await()
        Log.d("Test", "Register response time: $duration ms")
        assertTrue("Register failed or took too long", result)
    }

    @Test
    fun testSignInWithGoogleUnder3Second() = runBlocking {
        val auth = FirebaseAuth.getInstance()

        val dummyIdToken = FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
        val credential = GoogleAuthProvider.getCredential(dummyIdToken.toString(), null)

        val startTime = System.currentTimeMillis()

        val task = auth.signInWithCredential(credential)
        while (!task.isComplete) {
            // wait
        }

        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        println("Google Sign-In took $duration ms")
        assertTrue("Sign-In took too long!", duration < 3000)
    }

    @Before
    fun loginBeforeEachTest() {
        val latch = CountDownLatch(1)
        val email = "chyperxvagos@gmail.com"
        val password = "akuganteng4"

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                latch.countDown()
            }

        latch.await()

        assertNotNull("Login gagal sebelum test dimulai!", FirebaseAuth.getInstance().currentUser)
    }

    @Test
    fun testHomeFragmentResponseTime() {
        val startTime = System.currentTimeMillis()
        val latch = CountDownLatch(1)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        assertNotNull("User ID null, pastikan sudah login sebelum test!", userId)

        val ref = Firebase.database.reference.child("MealPlanData").child(userId!!)

        ref.get().addOnSuccessListener { snapshot ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            Log.d("Test", "HomeFragment response time: $duration ms")
            Log.d("Test", "Data snapshot: ${snapshot.value}")

            assertTrue("HomeFragment response > 3000ms", duration < 3000)

            assertTrue("Data jadwal tidak ditemukan!", snapshot.exists())

            latch.countDown()
        }.addOnFailureListener { error ->
            fail("Gagal mengambil data jadwal: ${error.message}")
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun testFoodFragmentResponseTime() {
        val startTime = System.currentTimeMillis()

        val latch = CountDownLatch(1)

        val ref = Firebase.database.reference.child("Food")
        ref.get().addOnSuccessListener { snapshot ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            Log.d("Test", "FoodFragment response time: $duration ms")
            Log.d("Test", "Data snapshot: ${snapshot.value}")

            assertTrue("FoodFragment response > 3000ms", duration < 3000)
            latch.countDown()
        }.addOnFailureListener {
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun testScheduleFragmentResponseTime() {
        val startTime = System.currentTimeMillis()

        val latch = CountDownLatch(1)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        assertNotNull("User ID null, pastikan sudah login sebelum test!", userId)

        val ref = Firebase.database.reference.child("MealPlanData").child(userId!!)

        ref.get().addOnSuccessListener { snapshot ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            Log.d("Test", "ScheduleFragment response time: $duration ms")
            Log.d("Test", "Data snapshot: ${snapshot.value}")

            assertTrue("ScheduleFragment response > 3000ms", duration < 3000)
            latch.countDown()
        }.addOnFailureListener {
            latch.countDown()
        }

        latch.await()
    }

    @Test
    fun testProfileFragmentResponseTime() {
        val startTime = System.currentTimeMillis()
        val latch = CountDownLatch(1)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        assertNotNull("User ID null, pastikan sudah login sebelum test!", userId)

        val ref = Firebase.database.reference.child("UserData").child(userId!!)

        ref.get()
            .addOnSuccessListener { snapshot ->
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                Log.d("Test", "ProfileFragment response time: $duration ms")
                Log.d("Test", "Data snapshot: ${snapshot.value}")

                assertTrue("ProfileFragment response > 3000ms", duration < 3000)

                assertTrue("Data user tidak ditemukan!", snapshot.exists())

                latch.countDown()
            }
            .addOnFailureListener { error ->
                fail("Gagal mengambil data user: ${error.message}")
                latch.countDown()
            }

        latch.await()
    }
}