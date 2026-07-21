package com.awsquiz.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.awsquiz.app.ui.history.HistoryScreen
import com.awsquiz.app.ui.home.HomeScreen
import com.awsquiz.app.ui.quiz.QuizScreen
import com.awsquiz.app.ui.result.ResultScreen

object Routes {
    const val HOME = "home"
    const val QUIZ = "quiz/{sessionId}"
    const val RESULT = "result/{correctCount}/{totalCount}/{percentage}/{passed}"
    const val HISTORY = "history"

    fun quiz(sessionId: String) = "quiz/$sessionId"
    fun result(correctCount: Int, totalCount: Int, percentage: Int, passed: Boolean) =
        "result/$correctCount/$totalCount/$percentage/$passed"
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToQuiz = { sessionId ->
                    navController.navigate(Routes.quiz(sessionId))
                },
                onNavigateToHistory = {
                    navController.navigate(Routes.HISTORY)
                }
            )
        }

        composable(
            route = Routes.QUIZ,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            QuizScreen(
                onSessionCompleted = { correctCount, totalCount, percentage, passed ->
                    navController.navigate(Routes.result(correctCount, totalCount, percentage, passed)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onError = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(
                navArgument("correctCount") { type = NavType.IntType },
                navArgument("totalCount") { type = NavType.IntType },
                navArgument("percentage") { type = NavType.IntType },
                navArgument("passed") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val correctCount = backStackEntry.arguments?.getInt("correctCount") ?: 0
            val totalCount = backStackEntry.arguments?.getInt("totalCount") ?: 0
            val percentage = backStackEntry.arguments?.getInt("percentage") ?: 0
            val passed = backStackEntry.arguments?.getBoolean("passed") ?: false

            ResultScreen(
                correctCount = correctCount,
                totalCount = totalCount,
                percentage = percentage,
                passed = passed,
                onReturnHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateHome = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                }
            )
        }
    }
}
