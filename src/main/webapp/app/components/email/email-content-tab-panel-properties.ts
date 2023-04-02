import {Email} from "../../models/email";

export interface EmailContentTabPanelProperties {
    email: Email
    activeContentType: string
    data: string
}