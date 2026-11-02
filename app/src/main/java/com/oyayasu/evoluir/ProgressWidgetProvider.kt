package com.oyayasu.evoluir

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import java.time.LocalDate

class ProgressWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateOne(context, manager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MARK) {
            val id = intent.getLongExtra(EXTRA_ID, -1L)
            val outcome = intent.getIntExtra(EXTRA_OUTCOME, -1)
            if (id > 0 && outcome in 0..1) {
                ImprovementStore(context).use { store ->
                    // The chosen day is the day of the actual tap, not when the widget was rendered.
                    store.mark(id, LocalDate.now().toString(), outcome)
                }
                refreshAll(context)
            }
        }
    }

    companion object {
        private const val ACTION_MARK = "com.oyayasu.evoluir.ACTION_MARK"
        private const val EXTRA_ID = "improvement_id"
        private const val EXTRA_OUTCOME = "outcome"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ProgressWidgetProvider::class.java))
            ids.forEach { updateOne(context, manager, it) }
        }

        private fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val items = ImprovementStore(context).use { it.list().take(4) }
            val views = RemoteViews(context.packageName, R.layout.widget_progress)
            views.removeAllViews(R.id.widget_rows)
            views.setViewVisibility(R.id.widget_empty, if (items.isEmpty()) View.VISIBLE else View.GONE)
            val open = PendingIntent.getActivity(
                context, widgetId, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, open)

            items.forEach { item ->
                val row = RemoteViews(context.packageName, R.layout.widget_row)
                row.setTextViewText(R.id.row_name, item.improvement.name)
                row.setImageViewResource(R.id.row_icon, IconCatalog.res(item.improvement.symbol))
                row.setInt(R.id.row_icon, "setColorFilter", ProgressPalette.forScore(item.score()))
                val todayOutcome = item.markOn(LocalDate.now().toString())?.outcome
                // Today's selected button is bold and the other one is muted.
                row.setTextColor(R.id.row_fail, if (todayOutcome == 0) 0xFFDC514F.toInt() else 0xFF929992.toInt())
                row.setTextColor(R.id.row_pass, if (todayOutcome == 1) 0xFF409B6B.toInt() else 0xFF929992.toInt())
                for (outcome in 0..1) {
                    val intent = Intent(context, ProgressWidgetProvider::class.java).apply {
                        action = ACTION_MARK
                        data = Uri.parse("evoluir://widget/$widgetId/${item.improvement.id}/$outcome")
                        putExtra(EXTRA_ID, item.improvement.id)
                        putExtra(EXTRA_OUTCOME, outcome)
                    }
                    val pending = PendingIntent.getBroadcast(
                        context, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    row.setOnClickPendingIntent(if (outcome == 0) R.id.row_fail else R.id.row_pass, pending)
                }
                views.addView(R.id.widget_rows, row)
            }
            manager.updateAppWidget(widgetId, views)
        }
    }
}
