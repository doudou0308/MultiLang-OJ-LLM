<template>
  <div ref="editorContainer" class="w-full h-full"></div>
</template>

<script setup lang="ts">
import { CODE_TEMPLATES } from '@/config/config'
import debounce from 'lodash/debounce'
import * as monaco from 'monaco-editor'
import { inject, onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'

const code = inject<Ref<string>>('code', ref(CODE_TEMPLATES.Java))
const language = inject<Ref<string>>('language', ref('Java'))

const editorContainer = ref<HTMLDivElement | null>(null)
let editor: monaco.editor.IStandaloneCodeEditor | null = null

onMounted(async () => {
  if (editorContainer.value) {
    editor = monaco.editor.create(editorContainer.value, {
      value: code.value,
      language: language.value.toLowerCase(),
      theme: 'vs-light',
      automaticLayout: true,
      scrollBeyondLastLine: false,
      scrollbar: {
        horizontalScrollbarSize: 8,
        verticalScrollbarSize: 8,
      },
      minimap: {
        enabled: false,
      },
    })

    const updateCode = debounce((newCode: string) => {
      code.value = newCode
    }, 500)

    editor.onDidChangeModelContent(() => {
      updateCode(editor?.getValue() || '')
    })
  }
})

watch(language, (lang) => {
  if (editor && lang) {
    const template = CODE_TEMPLATES[lang] || CODE_TEMPLATES.Java
    const monaco = editor.getModel()
    if (monaco) {
      monaco.setValue(template)
      monaco.setLanguage(lang.toLowerCase())
    }
  }
})

onBeforeUnmount(() => {
  if (editor) {
    editor.dispose()
  }
})
</script>
