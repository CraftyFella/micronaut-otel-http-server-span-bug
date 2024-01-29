package com.example.helpers

import io.opentelemetry.sdk.trace.data.SpanData

object SpanPrinter {

    fun printSpanTreeWithHeaderAndFooter(spans: List<SpanData>) {
        val width = 150
        val header = "=".repeat(width)

        println(header)

        findRoots(spans).forEach { root ->
            val currentSpanLine = "${root.name} - TraceID: ${root.traceId}, ParentID: ${root.parentSpanId}, SpanID: ${root.spanId}, Kind: ${root.kind}, Status: ${root.status.statusCode}, Attributes Count: ${root.attributes?.size()}"
            println(currentSpanLine)
            printSpanTree(spans, root, "", width)
        }

        println(header)
    }

    private fun findRoots(spans: List<SpanData>): List<SpanData> {
        return spans.filter { span ->
            spans.none { it.spanId == span.parentSpanId } || span.parentSpanId == null
        }
    }

    private fun printSpanTree(spans: List<SpanData>, currentSpan: SpanData, prefix: String = "", treeWidth: Int) {
        val childSpans = spans.filter { it.parentSpanId == currentSpan.spanId }

        childSpans.forEachIndexed { index, span ->
            val isLast = index == childSpans.size - 1
            val branch = if (isLast) "└─ " else "├─ "
            val linePrefix = if (isLast) "    " else "│   "

            val spanLine = "$prefix$branch${span.name} - SpanID: ${span.spanId}, Kind: ${span.kind}, Status: ${span.status.statusCode}, Attributes Count: ${span.attributes?.size()}"
            println(spanLine)

            printSpanTree(spans, span, prefix + linePrefix, treeWidth)
        }
    }
}
