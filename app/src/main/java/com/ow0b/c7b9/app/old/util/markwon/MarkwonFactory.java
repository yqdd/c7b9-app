package com.ow0b.c7b9.app.old.util.markwon;

import android.content.Context;

import androidx.annotation.NonNull;

import com.ow0b.pianoapp.util.markwon.GrammarLocatorImpl;

import org.commonmark.node.FencedCodeBlock;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.ext.tables.TablePlugin;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.syntax.Prism4jThemeDefault;
import io.noties.markwon.syntax.SyntaxHighlightPlugin;
import io.noties.prism4j.Prism4j;

public class MarkwonFactory
{
    public static Markwon getNormalInstance(Context context)
    {
        return Markwon.builder(context).usePlugin(TablePlugin.create(context)).build();
    }
    public static Markwon getSyntaxInstance(Context context)
    {
        return Markwon.builder(context)
                .usePlugin(SyntaxHighlightPlugin.create(new Prism4j(new GrammarLocatorImpl()), Prism4jThemeDefault.create(0)))
                .usePlugin(HtmlPlugin.create())
                .usePlugin(new AbstractMarkwonPlugin()
                {
                    @Override public void configureVisitor(@NonNull MarkwonVisitor.Builder builder)
                    {
                        builder.on(FencedCodeBlock.class, (visitor, block) ->
                        {
                            CharSequence code = visitor.configuration()
                                    .syntaxHighlight()
                                    .highlight(block.getInfo(), block.getLiteral().trim());
                            visitor.builder().append(code);
                        });
                    }
                })
                .build();
    }
}
