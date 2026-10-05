package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.AdCampaign
import com.example.data.model.AdPackage
import com.example.data.model.AdPaymentRecord
import com.example.data.model.AdQuotation
import com.example.data.model.AiAdActionLog
import com.example.data.model.AuraMemory
import com.example.data.model.ChatMessage
import com.example.data.model.ClientCommunicationMessage
import com.example.data.model.ClientProspect
import com.example.data.model.DailyCheckIn
import com.example.data.model.Exercise
import com.example.data.model.HealthActivityEntity
import com.example.data.model.HealthPrivacySettingsEntity
import com.example.data.model.HealthSummaryEntity
import com.example.data.model.NutritionLog
import com.example.data.model.OwnerApprovalRequest
import com.example.data.model.PricingRuleEntity
import com.example.data.model.UserProfile
import com.example.data.model.WaterLog
import com.example.data.model.WorkoutLog
import com.example.data.model.WorkoutPlan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        AuraMemory::class,
        Exercise::class,
        WorkoutPlan::class,
        WorkoutLog::class,
        NutritionLog::class,
        WaterLog::class,
        DailyCheckIn::class,
        ChatMessage::class,
        HealthSummaryEntity::class,
        HealthActivityEntity::class,
        HealthPrivacySettingsEntity::class,
        ClientProspect::class,
        AdPackage::class,
        PricingRuleEntity::class,
        AdQuotation::class,
        OwnerApprovalRequest::class,
        AdCampaign::class,
        AdPaymentRecord::class,
        ClientCommunicationMessage::class,
        AiAdActionLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun auraMemoryDao(): AuraMemoryDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun dailyCheckInDao(): DailyCheckInDao
    abstract fun chatDao(): ChatDao
    abstract fun healthDao(): HealthDao
    abstract fun adsDao(): AdsDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "umr_aura_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }
        }

        suspend fun populateInitialData(database: AuraDatabase) {
            val exerciseDao = database.exerciseDao()
            val memoryDao = database.auraMemoryDao()
            val chatDao = database.chatDao()
            val userDao = database.userDao()
            val workoutDao = database.workoutDao()

            if (exerciseDao.getCount() == 0) {
                exerciseDao.insertAll(initialExercises)
            }

            // Seed initial greeting from AURA
            chatDao.insertMessage(
                ChatMessage(
                    sender = "aura",
                    message = "Welcome to UMR AURA. I am AURA, your Personal AI Fitness Agent.\n\nOur philosophy is simple: LEAVE BETTER.\n\nI'm ready to craft your workouts, refine your form, optimize your nutrition, and track your progress. How can I coach you today?",
                    timestamp = System.currentTimeMillis()
                )
            )

            // Seed initial memory
            memoryDao.insertMemory(
                AuraMemory(
                    category = "Philosophy",
                    key = "Brand Mandate",
                    value = "LEAVE BETTER — Every workout, meal, and habit should elevate physical and mental performance."
                )
            )

            // Seed AURA ADS initial data
            val adsDao = database.adsDao()
            if (adsDao.getPricingRulesOnce() == null) {
                adsDao.savePricingRules(
                    PricingRuleEntity(
                        ownerPhone = "8309596486",
                        minCampaignPrice = 4000.0,
                        baseStartingPrice = 5000.0,
                        maxDiscountPercent = 20.0,
                        serviceFeePercent = 15.0,
                        creativeProductionFee = 1200.0,
                        managementFeePercent = 10.0,
                        minProfitMarginPercent = 25.0,
                        autonomousMode = "ASSISTED"
                    )
                )

                adsDao.insertPackage(
                    AdPackage(
                        name = "BASIC",
                        description = "Essential local visibility & digital awareness package.",
                        baseAdSpend = 3500.0,
                        serviceFee = 700.0,
                        creativeFee = 800.0,
                        platformCost = 0.0,
                        durationDays = 7,
                        featuresJson = """["7-Day Targeted Social Ad","Standard Single Creative","Local Geo-Targeting (5-10km)","Daily Performance Tracking","Basic Campaign Report"]"""
                    )
                )
                adsDao.insertPackage(
                    AdPackage(
                        name = "STANDARD",
                        description = "Comprehensive multi-platform campaign with creative variations.",
                        baseAdSpend = 7000.0,
                        serviceFee = 1500.0,
                        creativeFee = 1200.0,
                        platformCost = 300.0,
                        durationDays = 14,
                        featuresJson = """["14-Day Dual-Platform Ad (Meta + Google)","3 High-Converting Creatives","Demographic & Interest Retargeting","A/B Testing & Optimization","Detailed Analytics & Weekly Reviews"]"""
                    )
                )
                adsDao.insertPackage(
                    AdPackage(
                        name = "PREMIUM",
                        description = "High-performance omni-channel growth engine with video production.",
                        baseAdSpend = 15000.0,
                        serviceFee = 3000.0,
                        creativeFee = 2000.0,
                        platformCost = 500.0,
                        durationDays = 30,
                        featuresJson = """["30-Day Omni-Channel Campaign (Meta, Google, YouTube)","Full Video & Banner Creative Production","Hyper-Local & Retargeting Funnel","Continuous AI Performance Calibration","Dedicated Account Strategist & Real-time ROI Dashboard"]"""
                    )
                )

                adsDao.insertProspect(
                    ClientProspect(
                        businessName = "Apex Fitness Hub",
                        category = "Gym",
                        contactPerson = "Vikram Sharma",
                        contactPhone = "+91 98765 43210",
                        contactEmail = "contact@apexfitness.in",
                        cityLocation = "Koramangala, Bengaluru",
                        targetAudience = "Fitness enthusiasts, 20-40 yrs, high disposable income",
                        adObjective = "Lead Generation",
                        preferredPlatforms = "Meta Ads & Google Ads",
                        approxBudget = 10000.0,
                        expectedDurationDays = 14,
                        status = "INTERESTED",
                        notes = "Interested in summer membership campaign. Requested custom package."
                    )
                )
                adsDao.insertProspect(
                    ClientProspect(
                        businessName = "Verde Kitchen Bistro",
                        category = "Restaurant",
                        contactPerson = "Ananya Roy",
                        contactPhone = "+91 98111 22334",
                        contactEmail = "hello@verdekitchen.com",
                        cityLocation = "Indiranagar, Bengaluru",
                        targetAudience = "Foodies, weekend diners, 22-38 yrs within 6km",
                        adObjective = "Store Footfall",
                        preferredPlatforms = "Meta Ads",
                        approxBudget = 5000.0,
                        expectedDurationDays = 7,
                        status = "CONTACTED",
                        notes = "Opening new rooftop seating. Needs weekend awareness ads."
                    )
                )
                adsDao.insertProspect(
                    ClientProspect(
                        businessName = "Lumina Luxury Boutique",
                        category = "Retail",
                        contactPerson = "Rahul Mehta",
                        contactPhone = "+91 99200 33445",
                        contactEmail = "info@luminaboutique.com",
                        cityLocation = "Bandra West, Mumbai",
                        targetAudience = "Luxury shoppers, premium lifestyle, 25-50 yrs",
                        adObjective = "Brand Awareness",
                        preferredPlatforms = "Meta Ads & YouTube",
                        approxBudget = 25000.0,
                        expectedDurationDays = 30,
                        status = "PROSPECT",
                        notes = "Discovered via public business directory. High potential client."
                    )
                )

                adsDao.insertActionLog(
                    AiAdActionLog(
                        actionType = "SYSTEM_INITIALIZED",
                        description = "AURA ADS AGENT initialized. Ready for client acquisition, pricing calculations, and campaign management."
                    )
                )
            }
            memoryDao.insertMemory(
                AuraMemory(
                    category = "Safety",
                    key = "Injury Prevention",
                    value = "Prioritize controlled eccentric cadence, neutral spine, and adequate recovery."
                )
            )

            // Seed default ready-to-go workouts
            workoutDao.insertWorkoutPlan(
                WorkoutPlan(
                    title = "AURA Upper Body Hypertrophy",
                    targetMuscle = "Chest, Back, Shoulders",
                    durationMinutes = 45,
                    difficulty = "Intermediate",
                    equipment = "Gym / Dumbbells",
                    warmupText = "5 mins arm circles, band pull-aparts, light push-ups, dynamic shoulder rolls.",
                    exercisesJson = """[
                        {"exerciseId":"barbell_bench_press","name":"Barbell Bench Press","targetMuscles":"Chest, Triceps","sets":4,"reps":"8-10","restSeconds":90,"instructions":"Retract scapulae, touch lower sternum, drive through chest."},
                        {"exerciseId":"bent_over_row","name":"Bent-Over Barbell Row","targetMuscles":"Lats, Rhomboids","sets":4,"reps":"10-12","restSeconds":75,"instructions":"Hinge at hips, pull bar to upper navel, squeeze lats."},
                        {"exerciseId":"dumbbell_shoulder_press","name":"Seated Dumbbell Shoulder Press","targetMuscles":"Deltoids, Triceps","sets":3,"reps":"10-12","restSeconds":60,"instructions":"Press overhead in slight arc, avoid arched lower back."},
                        {"exerciseId":"dumbbell_bicep_curl","name":"Incline Dumbbell Bicep Curl","targetMuscles":"Biceps","sets":3,"reps":"12-15","restSeconds":45,"instructions":"Keep elbows static, full stretch at bottom, squeeze at apex."}
                    ]""".trimIndent(),
                    cooldownText = "5 mins chest door-frame stretch, overhead tricep stretch, child's pose.",
                    safetyNotes = "Keep core braced. If shoulder discomfort occurs, switch to neutral grip dumbbell press."
                )
            )

            workoutDao.insertWorkoutPlan(
                WorkoutPlan(
                    title = "AURA High-Torque Leg Day",
                    targetMuscle = "Quads, Hamstrings, Glutes",
                    durationMinutes = 50,
                    difficulty = "Intermediate",
                    equipment = "Full Gym",
                    warmupText = "5 mins bike/walk, bodyweight squats, leg swings, glute bridges.",
                    exercisesJson = """[
                        {"exerciseId":"barbell_squat","name":"Barbell Back Squat","targetMuscles":"Quadriceps, Glutes","sets":4,"reps":"8-10","restSeconds":120,"instructions":"Break at knees and hips simultaneously, hit parallel depth, push knees out."},
                        {"exerciseId":"romanian_deadlift","name":"Romanian Deadlift (RDL)","targetMuscles":"Hamstrings, Glutes","sets":4,"reps":"10-12","restSeconds":90,"instructions":"Soft knee bend, push hips backward until deep hamstring stretch."},
                        {"exerciseId":"walking_lunges","name":"Dumbbell Walking Lunges","targetMuscles":"Quads, Calves","sets":3,"reps":"12/leg","restSeconds":60,"instructions":"Long stride, torso upright, back knee gently kissing floor."},
                        {"exerciseId":"standing_calf_raise","name":"Calf Raises","targetMuscles":"Calves","sets":4,"reps":"15-20","restSeconds":45,"instructions":"Pause for 2 seconds at deep stretch, explode up on toes."}
                    ]""".trimIndent(),
                    cooldownText = "5 mins quad stretch, seated hamstring reach, foam rolling calves.",
                    safetyNotes = "Never compromise lumbar alignment for depth. Use safety bars."
                )
            )

            workoutDao.insertWorkoutPlan(
                WorkoutPlan(
                    title = "AURA 30-Min Fast Core & Burn",
                    targetMuscle = "Full Body & Core",
                    durationMinutes = 30,
                    difficulty = "Beginner to All",
                    equipment = "Bodyweight / Mat",
                    warmupText = "3 mins jumping jacks, arm windmills, cat-cow stretch.",
                    exercisesJson = """[
                        {"exerciseId":"pushups","name":"Push-ups","targetMuscles":"Chest, Core","sets":3,"reps":"12-15","restSeconds":45,"instructions":"Plank alignment, elbows 45 degrees, chest near ground."},
                        {"exerciseId":"bodyweight_squat","name":"Air Squats","targetMuscles":"Quads, Glutes","sets":3,"reps":"20","restSeconds":45,"instructions":"Full depth, explosive drive upward."},
                        {"exerciseId":"plank","name":"Forearm Plank Hold","targetMuscles":"Core, Abdominals","sets":3,"reps":"45s hold","restSeconds":45,"instructions":"Tuck pelvis, brace abdomen as if expecting a punch."},
                        {"exerciseId":"mountain_climbers","name":"Mountain Climbers","targetMuscles":"Core, Cardio","sets":3,"reps":"30s active","restSeconds":45,"instructions":"Rapid alternating knee drives with stable hips."}
                    ]""".trimIndent(),
                    cooldownText = "Cobra stretch, child's pose, deep diaphragmatic breathing.",
                    safetyNotes = "Maintain steady breathing; do not hold breath during core holds."
                )
            )
        }

        val initialExercises = listOf(
            Exercise(
                id = "barbell_bench_press",
                name = "Barbell Bench Press",
                muscleGroup = "Chest",
                secondaryMuscles = "Triceps, Anterior Deltoids",
                difficulty = "Intermediate",
                equipment = "Barbell, Flat Bench",
                instructions = "Lie on bench with eyes under bar. Grip slightly wider than shoulder-width. Unrack, lower bar with control to mid-chest, drive up with feet planted firmly on ground.",
                commonMistakes = "Bouncing bar off sternum, flaring elbows out at 90 degrees, lifting hips off bench.",
                safetyNotes = "Always keep wrists neutral and use safety catches or a spotter.",
                defaultSets = 4,
                defaultReps = "8-10",
                restSeconds = 90
            ),
            Exercise(
                id = "pushups",
                name = "Standard Push-up",
                muscleGroup = "Chest",
                secondaryMuscles = "Core, Triceps, Shoulders",
                difficulty = "Beginner",
                equipment = "Bodyweight",
                instructions = "Place hands shoulder-width apart, fingers slightly splayed. Form a rigid straight line from heels to head. Lower chest until 2 inches from floor, push back up forcefully.",
                commonMistakes = "Sagging lower back, flaring elbows wide, only doing partial half-reps.",
                safetyNotes = "Keep glutes and core clenched to protect lumbar spine.",
                defaultSets = 3,
                defaultReps = "12-15",
                restSeconds = 60
            ),
            Exercise(
                id = "incline_dumbbell_press",
                name = "Incline Dumbbell Press",
                muscleGroup = "Chest",
                secondaryMuscles = "Upper Chest, Anterior Deltoids, Triceps",
                difficulty = "Intermediate",
                equipment = "Dumbbells, Incline Bench (30-45 deg)",
                instructions = "Sit on incline bench. Kick dumbbells up to shoulder level. Press straight upward above upper chest, converging gently without clanging. Control descent.",
                commonMistakes = "Setting incline too steep (>45 deg shifts load to delts), dropping elbows too low.",
                safetyNotes = "Do not drop weights suddenly; bring dumbbells to knees before sitting up.",
                defaultSets = 3,
                defaultReps = "10-12",
                restSeconds = 75
            ),
            Exercise(
                id = "barbell_squat",
                name = "Barbell Back Squat",
                muscleGroup = "Legs",
                secondaryMuscles = "Glutes, Hamstrings, Core, Spinal Erectors",
                difficulty = "Intermediate",
                equipment = "Barbell, Squat Rack",
                instructions = "Rest bar on upper traps. Feet shoulder-width apart, toes angled out 15-30 degrees. Inhale, brace core, break at hips and knees simultaneously. Descend to parallel, then explode upward driving through mid-foot.",
                commonMistakes = "Knees caving inward (valgus collapse), weight shifting to toes, rounded lower back (butt wink).",
                safetyNotes = "Always set safety pins just below bottom depth. Never perform with improper footwear.",
                defaultSets = 4,
                defaultReps = "8-10",
                restSeconds = 120
            ),
            Exercise(
                id = "bodyweight_squat",
                name = "Air Squat",
                muscleGroup = "Legs",
                secondaryMuscles = "Glutes, Core",
                difficulty = "Beginner",
                equipment = "Bodyweight",
                instructions = "Stand tall with feet shoulder-width apart. Extend arms forward for counter-balance. Descend until hips break parallel, drive back to full hip extension.",
                commonMistakes = "Heels rising off floor, collapsing chest forward.",
                safetyNotes = "Focus on tracking knees over toes.",
                defaultSets = 3,
                defaultReps = "15-20",
                restSeconds = 45
            ),
            Exercise(
                id = "romanian_deadlift",
                name = "Romanian Deadlift (RDL)",
                muscleGroup = "Legs",
                secondaryMuscles = "Hamstrings, Glutes, Lower Back, Forearms",
                difficulty = "Intermediate",
                equipment = "Barbell or Dumbbells",
                instructions = "Stand holding weight at hips with overhand grip. Keep knees softly unlocked. Push hips straight back as bar glides down shins, maintaining a flat back. Squeeze glutes to return.",
                commonMistakes = "Squatting the weight down rather than hinging, rounding upper or lower back.",
                safetyNotes = "Halt descent when hips stop moving backward to prevent lower back loading.",
                defaultSets = 3,
                defaultReps = "10-12",
                restSeconds = 90
            ),
            Exercise(
                id = "bent_over_row",
                name = "Bent-Over Barbell Row",
                muscleGroup = "Back",
                secondaryMuscles = "Lats, Rhomboids, Biceps, Posterior Deltoids",
                difficulty = "Intermediate",
                equipment = "Barbell",
                instructions = "Hinge forward at roughly 45 degrees with flat spine. Grip bar shoulder-width. Pull bar directly to lower ribcage/navel, squeezing shoulder blades together at top.",
                commonMistakes = "Jerking torso upright with momentum, hunching cervical spine.",
                safetyNotes = "Brace abs tightly to lock pelvic angle.",
                defaultSets = 4,
                defaultReps = "8-12",
                restSeconds = 75
            ),
            Exercise(
                id = "pullups",
                name = "Pull-up",
                muscleGroup = "Back",
                secondaryMuscles = "Lats, Biceps, Forearms, Upper Back",
                difficulty = "Advanced",
                equipment = "Pull-up Bar",
                instructions = "Take overhand grip slightly wider than shoulders. Hang in dead hang, depress scaps, pull body up until chin clears bar. Lower under control.",
                commonMistakes = "Kipping legs uncontrollably, shrugging shoulders to ears, partial range of motion.",
                safetyNotes = "Use resistance band assistance if unable to complete 5 strict repetitions.",
                defaultSets = 3,
                defaultReps = "6-10",
                restSeconds = 90
            ),
            Exercise(
                id = "lat_pulldown",
                name = "Cable Lat Pulldown",
                muscleGroup = "Back",
                secondaryMuscles = "Lats, Biceps, Rhomboids",
                difficulty = "Beginner",
                equipment = "Cable Machine",
                instructions = "Secure thighs beneath pad. Grasp bar wide. Lean back 10-15 degrees. Pull bar smoothly down to upper chest while driving elbows down and back.",
                commonMistakes = "Pulling bar behind neck (excessive cervical strain), excessive swinging.",
                safetyNotes = "Control the stack weight on release, do not let it yank shoulders.",
                defaultSets = 3,
                defaultReps = "10-12",
                restSeconds = 60
            ),
            Exercise(
                id = "overhead_press",
                name = "Standing Overhead Military Press",
                muscleGroup = "Shoulders",
                secondaryMuscles = "Anterior Deltoids, Triceps, Upper Chest, Core",
                difficulty = "Intermediate",
                equipment = "Barbell",
                instructions = "Hold bar at collarbone with hands just outside shoulders. Tighten glutes and core. Press bar vertically in a straight path, moving head back slightly then forward as bar clears.",
                commonMistakes = "Excessive lumbar hyperextension, turning press into an incline chest press.",
                safetyNotes = "Lock ribcage down to avoid pinching lower lumbar discs.",
                defaultSets = 3,
                defaultReps = "8-10",
                restSeconds = 90
            ),
            Exercise(
                id = "lateral_raise",
                name = "Dumbbell Lateral Raise",
                muscleGroup = "Shoulders",
                secondaryMuscles = "Lateral Deltoids, Trapezius",
                difficulty = "Beginner",
                equipment = "Dumbbells",
                instructions = "Hold dumbbells at sides with slight forward lean. Raise arms out to sides in scaption plane (30 deg forward) until parallel to ground, lead with elbows.",
                commonMistakes = "Using heavy momentum and body swing, raising weights higher than shoulder plane.",
                safetyNotes = "Keep slight bend in elbows to alleviate joint shear.",
                defaultSets = 4,
                defaultReps = "12-15",
                restSeconds = 45
            ),
            Exercise(
                id = "dumbbell_bicep_curl",
                name = "Dumbbell Bicep Curl",
                muscleGroup = "Arms",
                secondaryMuscles = "Biceps Brachii, Brachialis, Forearms",
                difficulty = "Beginner",
                equipment = "Dumbbells",
                instructions = "Hold dumbbells with palms forward. Keep upper arms stationary against torso. Curl weight up while squeezing biceps, lower slowly through full range.",
                commonMistakes = "Swinging elbows forward to cheat, leaning backwards.",
                safetyNotes = "Focus on mind-muscle connection and deliberate 2-second negative.",
                defaultSets = 3,
                defaultReps = "10-12",
                restSeconds = 45
            ),
            Exercise(
                id = "tricep_rope_pushdown",
                name = "Cable Tricep Rope Pushdown",
                muscleGroup = "Arms",
                secondaryMuscles = "Triceps Lateral & Medial Heads",
                difficulty = "Beginner",
                equipment = "Cable Machine, Rope Attachment",
                instructions = "Pin elbows to ribs. Push rope downward toward thighs, spreading ends of rope apart at the bottom to achieve peak tricep contraction.",
                commonMistakes = "Flaring elbows, using shoulders to push downward.",
                safetyNotes = "Do not let rope jerk elbows up into hyper-flexion.",
                defaultSets = 3,
                defaultReps = "12-15",
                restSeconds = 45
            ),
            Exercise(
                id = "plank",
                name = "Forearm Plank",
                muscleGroup = "Core",
                secondaryMuscles = "Transverse Abdominis, Shoulders, Glutes",
                difficulty = "Beginner",
                equipment = "Mat / Floor",
                instructions = "Rest on forearms with elbows directly under shoulders. Keep body in arrow-straight line from head to heels. Draw navel toward spine and hold.",
                commonMistakes = "Piking hips into air, allowing lower back to sag.",
                safetyNotes = "If back strains, lower knees to modify.",
                defaultSets = 3,
                defaultReps = "45-60s hold",
                restSeconds = 45
            ),
            Exercise(
                id = "hanging_leg_raise",
                name = "Hanging Knee/Leg Raise",
                muscleGroup = "Core",
                secondaryMuscles = "Rectus Abdominis, Hip Flexors",
                difficulty = "Intermediate",
                equipment = "Pull-up Bar",
                instructions = "Hang from pull-up bar. Without swinging, curl knees or straight legs up toward chest, tilting pelvis backward to engage lower abs fully.",
                commonMistakes = "Using pendular swinging momentum rather than muscular abdominal contraction.",
                safetyNotes = "Stabilize torso before initiating each repetition.",
                defaultSets = 3,
                defaultReps = "10-12",
                restSeconds = 60
            ),
            Exercise(
                id = "walking_lunges",
                name = "Walking Lunges",
                muscleGroup = "Legs",
                secondaryMuscles = "Quadriceps, Glutes, Hamstrings, Balance",
                difficulty = "Beginner",
                equipment = "Bodyweight or Dumbbells",
                instructions = "Step forward with right foot, lowering left knee until both knees form 90 degree angles. Push through front heel to step forward into next lunge.",
                commonMistakes = "Front knee projecting drastically past toes, leaning torso excessively.",
                safetyNotes = "Maintain upright torso and hip stability throughout stride.",
                defaultSets = 3,
                defaultReps = "12 reps/leg",
                restSeconds = 60
            )
        )
    }
}
