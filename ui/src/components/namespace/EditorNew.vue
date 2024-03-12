<template>
    <top-nav-bar :title="routeInfo.title">
        <template #additional-right>
            <namespace-select
                class="fit-content"
                data-type="flow"
                :value="namespace"
                @update:model-value="namespaceUpdate"
                allow-create
                :is-filter="false"
            />
        </template>
    </top-nav-bar>
    <div v-if="namespace" class="editor-wrapper">
        <el-button
            :icon="FolderPlus"
            class="p-2 m-0"
            @click="showAddFolderOrFile('folder')"
        />
        <el-button
            :icon="FilePlus"
            class="p-2 m-0"
            @click="showAddFolderOrFile('file')"
        />
        <el-tree
            style="max-width: 600px"
            :data="foldersAsTree"
            @node-click="handleNodeClick"
            :render-content="renderTree"
        />
        <editor
            class="editor"
            :full-height="false"
            :input="false"
            v-model="content"
            :navbar="true"
            highlight
            :lang="extensionToMonacoLang"
        />
    </div>
    <section v-else class="container">
        <el-alert type="info" :closable="false">
            {{ $t("namespace choice") }}
        </el-alert>
    </section>
    <el-dialog
        :title="modalTitle"
        v-model="shouldShowAddFolderOrFile"
        width="30%"
    >
        <el-form label-position="top" :rules="rules" :model="form" ref="form" @submit.prevent="false">
            <el-form-item
                :label="modalInputText"
                required
                prop="name"
            >
                <el-input props="name" v-model="form.name" />
            </el-form-item>
            <el-form-item
                v-if="shouldShowAddFolderOrFile === 'file'"
                :label="$t('fileContent')"
                required
                prop="fileContent"
            >
                <input
                    type="file"
                    action="#"
                    @change="onFileContentUpload()"
                    ref="file"
                >
            </el-form-item>
            <div class="bottom-buttons">
                <div class="right-align">
                    <el-form-item class="submit">
                        <el-button @click="onFileOrFolderSubmit($refs.form)" type="primary" native-type="submit">
                            {{ $t('save') }}
                        </el-button>
                    </el-form-item>
                </div>
            </div>
        </el-form>
    </el-dialog>
</template>

<script setup>
    import NamespaceSelect from "./NamespaceSelect.vue";
    import TopNavBar from "../layout/TopNavBar.vue";
    import Editor from "../inputs/Editor.vue";
</script>

<script>
    import FolderPlus from "vue-material-design-icons/LockOff.vue";
    import FilePlus from "vue-material-design-icons/FilePlus.vue";

    import RouteContext from "../../mixins/routeContext";
    import RestoreUrl from "../../mixins/restoreUrl";
    import Monaco from "../../mixins/monaco";
    import {mapState} from "vuex";
    import {storageKeys} from "../../utils/constants";


    export default {
        mixins: [RouteContext, RestoreUrl, Monaco],
        async created() {
            if (!this.namespace) return;
            this.folders = this.folders = await this.getDirectoryContent(this.namespace);
        },
        watch: {
            async namespace() {
                this.folders = this.folders = await this.getDirectoryContent(this.namespace);
            }
        },
        methods: {
            onFileContentUpload() {
                const formData = new FormData();
                formData.append("fileContent", this.$refs.file.files[0]);
                this.form.fileContent = formData;
            },
            onFileOrFolderSubmit(formRef) {
                return formRef.validate(async (valid) => {
                    try {
                        if (!valid) {
                            return false;
                        }

                        if (this.shouldShowAddFolderOrFile === "file") {
                            await this.$store.dispatch("namespace/createFile", {
                                namespace: this.namespace,
                                path: this.form.name,
                                fileContent: this.form.fileContent
                            });
                        } else {
                            await this.$store.dispatch("namespace/createFolder", {
                                namespace: this.namespace,
                                path: this.form.name
                            });
                        }
                        await this.getDirectoryContent(this.namespace);
                        this.shouldShowAddFolderOrFile = false;
                        this.$toast.success(this.$t("namespace files.create.success"));
                    } catch (e) {
                        this.$toast.error(this.$t("namespace files.create.error"));
                    }

                });
            },
            showAddFolderOrFile(type) {
                this.shouldShowAddFolderOrFile = type;
            },
            renderTree(h, {data}) {
                return h("span", {
                    class: "custom-node"
                }, [
                    h("span", {
                        class: "custom-node-label",
                        "data-path": "toto"
                    }, data.label)
                ]);
            },
            async getDirectoryContent(namespace, path = null) {
                const {data} = await this.$store.dispatch("namespace/listDirectoryContent", {
                    namespace,
                    path
                });
                return Promise.all(data.map(async (node) => {
                    const fullPath = (path ? path + "/" : "/") + node.fileName;
                    node.text = fullPath
                    if (node.type === "File") return node;
                    const result = await this.getDirectoryContent(
                        namespace,
                        fullPath);
                    node.children = result;
                    return node;
                }));
            },
            async extractParentPath(node, suffix = "") {
                let fullPath = ""
                let currentNode = node.parent
                while (currentNode) {
                    if (!currentNode.data.label) {
                        currentNode = currentNode.parent
                        continue
                    }
                    fullPath = currentNode.data.label + "/" + fullPath
                    currentNode = currentNode.parent
                }
                fullPath = fullPath + suffix;
                return fullPath;
            },
            async handleNodeClick(data, node) {
                const fullPath = await this.extractParentPath(node, data.label)
                if (data.children.length > 0) return;
                const {data: fileContent} = await this.$store.dispatch("namespace/getFileContent", {
                    namespace: this.namespace,
                    path: "/" + fullPath
                });
                this.extension = data.label.split(".").pop();
                this.content = fileContent;
            },
            namespaceUpdate(namespace) {
                localStorage.setItem(storageKeys.LATEST_NAMESPACE, namespace);
                this.$router.push({
                    params: {
                        namespace
                    }
                });
            },
            treeMapper(node) {
                return {
                    label: node.fileName,
                    children: node.children ? node.children.map(this.treeMapper) : []
                }
            }
        },
        data() {
            return {
                file: undefined,
                form: {
                    name: undefined,
                    fileContent: undefined
                },
                shouldShowAddFolderOrFile: false,
                folders: [],
                content: "",
            }
        },
        computed: {
            modalInputText() {
                return this.shouldShowAddFolderOrFile === "file" ? this.$t("file name") : this.$t("folder name")
            },
            modalTitle() {
                return this.shouldShowAddFolderOrFile === "file" ? this.$t("add file") : this.$t("add folder")
            },
            ...mapState("namespace", ["namespaces"]),
            routeInfo() {
                return {
                    title: this.$t("editor")
                };
            },
            foldersAsTree() {
                return this.folders.map(this.treeMapper);
            },
            namespace() {
                return this.$route.params.namespace;
            }
        }
    }
</script>

<style lang="scss">
    .fit-content {
        width: fit-content;
    }
    .editor-wrapper {
    }
</style>