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
</template>

<script setup>
    import NamespaceSelect from "./NamespaceSelect.vue";
    import TopNavBar from "../layout/TopNavBar.vue";
    import Editor from "../inputs/Editor.vue";
</script>

<script>
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
                folders: [],
                content: "",
                flow: null,
                tabsNotSaved: [],
                uploadFileName: undefined
            }
        },
        computed: {
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