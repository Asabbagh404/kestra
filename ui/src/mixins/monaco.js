export default {
    data() {
        return {
            extension: ""
        }
    },
    computed: {
        extensionToMonacoLang() {
            switch (this.extension) {
            case "json":
                return "json";
            case "jsonl":
                return "jsonl";
            case "yaml":
            case "yml":
            case "ion":
                // little hack to get ion colored with monaco
                return "yaml";
            case "csv":
                return "csv";
            case "py":
                return "python"
            default:
                return this.extension;
            }
        },
    }
}