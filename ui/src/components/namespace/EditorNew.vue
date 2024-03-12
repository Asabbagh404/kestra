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
        />
        <editor
            :full-height="false"
            :input="false"
            v-model="content"
            :navbar="true"
            lang="json"
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
    import {mapState} from "vuex";
    import {storageKeys} from "../../utils/constants";


    export default {
        mixins: [RouteContext, RestoreUrl],
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
            async getDirectoryContent(namespace, path = null) {
                const {data} = await this.$store.dispatch("namespace/listDirectoryContent", {
                    namespace,
                    path
                });
                return Promise.all(data.map(async (node) => {
                    // test
                    if (node.type === "File") return node;
                    const result = await this.getDirectoryContent(
                        namespace,
                        (path ? path + "/" : "/") + node.fileName);
                    node.children = result;
                    return node;
                }));
            },
            async handleNodeClick(data) {
                console.log(data.label)
                if (data.children.length > 0) return;
                const {data: fileContent} = await this.$store.dispatch("namespace/getFileContent", {
                    namespace: this.namespace,
                    path: "/" + data.label
                });
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
                content: "test",
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
        display: flex;
        flex-direction: row;
        justify-content: space-between;
    }
</style>