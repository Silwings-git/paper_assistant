import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { ProjectDTO } from '@/types/project'
import { toProjectDTO, toProjectDtoList } from '@/types/project'
import { getProjects, createProject, updateProject, deleteProject, setProjectBasePaper } from '@/api'

export const useProjectStore = defineStore('project', () => {
  const projects = ref<ProjectDTO[]>([])
  const currentProject = ref<ProjectDTO | null>(null)
  const loading = ref(false)

  async function fetchList() {
    loading.value = true
    try {
      const raw = await getProjects()
      if (Array.isArray(raw)) {
        projects.value = toProjectDtoList(raw)
      }
    } catch {
      // 错误已由拦截器提示
    } finally {
      loading.value = false
    }
  }

  async function create(data: { name: string; description?: string; topic: string }) {
    const raw = await createProject(data)
    const res = toProjectDTO(raw)
    await fetchList()
    return res
  }

  async function update(id: string, data: Partial<{ name: string; description: string; topic: string }>) {
    const raw = await updateProject(id, data)
    const res = toProjectDTO(raw)
    await fetchList()
    return res
  }

  async function remove(id: string) {
    await deleteProject(id)
    await fetchList()
  }

  async function setBase(projectId: string, paperId: string) {
    const raw = await setProjectBasePaper(projectId, { paperId })
    await fetchList()
    return toProjectDTO(raw)
  }

  function setCurrent(project: ProjectDTO | null) {
    currentProject.value = project
  }

  return { projects, currentProject, loading, fetchList, create, update, remove, setBase, setCurrent }
})
